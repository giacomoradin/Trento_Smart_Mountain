// Database import script for Sentieri data
import "dotenv/config";
import mongoose from "mongoose";
import path from "path";
import { fileURLToPath } from 'url';
import { dirname } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
import KMLParser from '../backend/src/utils/KMLParser.js';
import Sentiero from '../backend/src/models/Sentiero.js';
/**
 * Import Script for SAT Mountain Pathways
 * 
 * This script:
 * 1. Connects to MongoDB
 * 2. Parses the sentierisat.kml file
 * 3. Imports all trails into the database
 * 4. Displays statistics about the import
 * 
 * Usage:
 *   node importSentieri.js
 * 
 * Environment Variables:
 *   MONGODB_URI - MongoDB connection string (default: mongodb://localhost:27017/sat-pathways)
 *   KML_FILE_PATH - Path to the KML file (default: ./data/sentierisat/sentierisat.kml)
 */

class SentieriImporter {
  constructor() {
  this.mongoUri = process.env.MONGO_URI || 'mongodb://localhost:27017/sat-pathways';
  this.kmlFilePath = process.env.KML_FILE_PATH || path.join(process.cwd(), 'data', 'sentierisat.kml', 'sentieri_tratte.kml');
  this.parser = new KMLParser();
}

  /**
   * Connect to MongoDB
   */
  async connect() {
  try {
    await mongoose.connect(this.mongoUri);
    console.log('✓ Connected to MongoDB');
  } catch (error) {
    console.error('✗ MongoDB connection error:', error);
    throw error;
  }
}

  /**
   * Disconnect from MongoDB
   */
  async disconnect() {
    await mongoose.disconnect();
    console.log('✓ Disconnected from MongoDB');
  }

  /**
   * Clear existing trail data
   */
  async clearExistingData() {
    const count = await Sentiero.countDocuments();
    if (count > 0) {
      console.log(`Found ${count} existing trails in database`);
      await Sentiero.deleteMany({});
      console.log('✓ Cleared existing trail data');
    } else {
      console.log('✓ No existing data to clear');
    }
  }

  /**
   * Import trails from KML file
   */
  async importTrails() {
    console.log(`\nParsing KML file: ${this.kmlFilePath}`);
    const trails = await this.parser.parseKML(this.kmlFilePath);
    
    console.log(`\nImporting ${trails.length} trails to database...`);
    
    let successCount = 0;
    let errorCount = 0;
    const errors = [];
    
    // Import trails in batches for better performance
    const batchSize = 100;
    for (let i = 0; i < trails.length; i += batchSize) {
      const batch = trails.slice(i, i + batchSize);
      
      try {
        await Sentiero.insertMany(batch, { ordered: false });
        successCount += batch.length;
        process.stdout.write(`\rProgress: ${successCount}/${trails.length}`);
      } catch (error) {
        // Handle duplicate key errors and other issues
        if (error.writeErrors) {
          error.writeErrors.forEach(writeError => {
            errorCount++;
            errors.push({
              codice: batch[writeError.index]?.codice,
              error: writeError.errmsg
            });
          });
          // Count successful inserts in this batch
          const successInBatch = batch.length - error.writeErrors.length;
          successCount += successInBatch;
        } else {
          errorCount += batch.length;
          errors.push({
            batch: `${i}-${i + batch.length}`,
            error: error.message
          });
        }
      }
    }
    
    console.log('\n');
    return { successCount, errorCount, errors };
  }

  /**
   * Display import statistics
   */
  async displayStatistics() {
    console.log('\n--- DATABASE STATISTICS ---');
    
    const totalCount = await Sentiero.countDocuments();
    console.log(`Total trails in database: ${totalCount}`);
    
    // Trails by difficulty
    const byDifficulty = await Sentiero.aggregate([
      { $group: { _id: '$difficolta', count: { $sum: 1 } } },
      { $sort: { _id: 1 } }
    ]);
    
    console.log('\nTrails by difficulty:');
    byDifficulty.forEach(({ _id, count }) => {
      console.log(`  ${_id}: ${count}`);
    });
    
    // Unique destinations
    const uniqueDestinations = await Sentiero.distinct('puntoFine.nome');
    console.log(`\nUnique destinations: ${uniqueDestinations.length}`);
    
    // Trail code prefixes (E vs O)
    const estCount = await Sentiero.countDocuments({ codice: /^E/ });
    const ovestCount = await Sentiero.countDocuments({ codice: /^O/ });
    console.log(`\nTrails by region:`);
    console.log(`  Est (E): ${estCount}`);
    console.log(`  Ovest (O): ${ovestCount}`);
    
    // Sample destinations with most trails
    const topDestinations = await Sentiero.aggregate([
      { $group: { _id: '$puntoFine.nome', count: { $sum: 1 } } },
      { $sort: { count: -1 } },
      { $limit: 5 }
    ]);
    
    console.log('\nTop 5 destinations (most trails):');
    topDestinations.forEach(({ _id, count }) => {
      console.log(`  ${_id}: ${count} trails`);
    });
  }

  /**
   * Main import process
   */
  async run() {
    try {
      console.log('=== SAT MOUNTAIN PATHWAYS IMPORTER ===\n');
      
      await this.connect();
      await this.clearExistingData();
      
      const { successCount, errorCount, errors } = await this.importTrails();
      
      console.log('\n--- IMPORT RESULTS ---');
      console.log(`✓ Successfully imported: ${successCount} trails`);
      
      if (errorCount > 0) {
        console.log(`✗ Failed to import: ${errorCount} trails`);
        console.log('\nErrors:');
        errors.slice(0, 10).forEach(err => {
          console.log(`  - ${err.codice || err.batch}: ${err.error}`);
        });
        if (errors.length > 10) {
          console.log(`  ... and ${errors.length - 10} more errors`);
        }
      }
      
      await this.displayStatistics();
      
      await this.disconnect();
      
      console.log('\n✓ Import completed successfully!');
      process.exit(0);
      
    } catch (error) {
      console.error('\n✗ Import failed:', error);
      await this.disconnect();
      process.exit(1);
    }
  }
}

// Run the importer if executed directly
// Run the importer if executed directly
const importer = new SentieriImporter();
importer.run();

export default SentieriImporter;
