
import fs from "fs";
import xml2js from "xml2js";
/**
 * KML Parser for SAT Mountain Pathways
 * 
 * Parses the sentierisat.kml file and extracts trail data into a structured format
 * compatible with the Sentiero Mongoose model.
 */

class KMLParser {
  constructor() {
    this.parser = new xml2js.Parser({
      explicitArray: false,
      mergeAttrs: true,
      normalizeTags: true,
      normalize: true,
      trim: true
    });
  }

  /**
   * Parse KML file and extract all trail placemarks
   * @param {string} filePath - Path to the KML file
   * @returns {Promise<Array>} Array of parsed trail objects
   */
  async parseKML(filePath) {
    try {
      const kmlContent = fs.readFileSync(filePath, 'utf-8');
      const result = await this.parser.parseStringPromise(kmlContent);
      
      // Navigate the KML structure to find placemarks
      const document = result.kml?.document || result.kml;
      const placemarks = this._extractPlacemarks(document);
      
      console.log(`Found ${placemarks.length} placemarks in KML`);
      
      const trails = placemarks
        .map(placemark => this._parsePlacemark(placemark))
        .filter(trail => trail !== null); // Filter out any invalid entries
      
      console.log(`Successfully parsed ${trails.length} valid trails`);
      
      return trails;
    } catch (error) {
      console.error('Error parsing KML file:', error);
      throw error;
    }
  }

  /**
   * Extract all Placemark elements from KML document
   * @private
   */
  _extractPlacemarks(document) {
    let placemarks = [];
    
    // KML structure can vary, handle different cases
    if (document.placemark) {
      placemarks = Array.isArray(document.placemark) 
        ? document.placemark 
        : [document.placemark];
    } else if (document.folder) {
      const folders = Array.isArray(document.folder) 
        ? document.folder 
        : [document.folder];
      
      folders.forEach(folder => {
        if (folder.placemark) {
          const folderPlacemarks = Array.isArray(folder.placemark)
            ? folder.placemark
            : [folder.placemark];
          placemarks.push(...folderPlacemarks);
        }
      });
    }
    
    return placemarks;
  }

  /**
   * Parse a single placemark into trail data
   * @private
   */
  _parsePlacemark(placemark) {
    try {
      // Extract ExtendedData
      const extendedData = placemark.extendeddata?.schemadata?.simpledata;
      
      if (!extendedData) {
        console.warn('Placemark missing extended data, skipping');
        return null;
      }
      
      // Convert array of SimpleData to object
      const data = {};
      const dataArray = Array.isArray(extendedData) ? extendedData : [extendedData];
      
      dataArray.forEach(item => {
        if (item.name && item._) {
          data[item.name] = item._;
        }
      });
      
      // Extract coordinates from LineString
      const coordinatesString = placemark.linestring?.coordinates;
      
      if (!coordinatesString) {
        console.warn(`Trail ${data.numero} missing coordinates, skipping`);
        return null;
      }
      
      // Parse coordinates
      const coords = this._parseCoordinates(coordinatesString);
      
      if (coords.length === 0) {
        console.warn(`Trail ${data.numero} has no valid coordinates, skipping`);
        return null;
      }
      
      // Extract first and last coordinates for start/end points
      const firstCoord = coords[0];
      const lastCoord = coords[coords.length - 1];
      
      // Build trail object matching Sentiero schema
      const trail = {
        codice: data.numero,
        denominazione: data.denominaz || '',
        
        puntoInizio: {
          nome: data.loc_inizio,
          quota: parseInt(data.quota_iniz, 10),
          coordinate: {
            lat: firstCoord.lat,
            lon: firstCoord.lon
          }
        },
        
        puntoFine: {
          nome: data.loc_fine,
          quota: parseInt(data.quota_fine, 10),
          coordinate: {
            lat: lastCoord.lat,
            lon: lastCoord.lon
          }
        },
        
        difficolta: data.difficolta,
        quotaMinima: parseInt(data.quota_min, 10),
        quotaMassima: parseInt(data.quota_max, 10),
        lunghezzaPlanimetrica: parseInt(data.lun_planim, 10),
        lunghezzaInclinata: parseInt(data.lun_inclin, 10),
        tempoAndata: data.t_andata,
        tempoRitorno: data.t_ritorno,
        
        competenza: data.competenza || '',
        gruppoMontano: data.gr_mont || '',
        comuniToccati: data.comuni_toc || '',
        
        // Store coordinates as compact string (same format as KML)
        percorsoCoordinate: coordinatesString.trim()
      };
      
      return trail;
      
    } catch (error) {
      console.error('Error parsing placemark:', error);
      return null;
    }
  }

  /**
   * Parse coordinate string from KML into array of {lat, lon} objects
   * @private
   */
  _parseCoordinates(coordString) {
    const coords = [];
    const pairs = coordString.trim().split(/\s+/);
    
    pairs.forEach(pair => {
      const parts = pair.split(',');
      if (parts.length >= 2) {
        const lon = parseFloat(parts[0]);
        const lat = parseFloat(parts[1]);
        
        if (!isNaN(lat) && !isNaN(lon)) {
          coords.push({ lat, lon });
        }
      }
    });
    
    return coords;
  }

  /**
   * Get unique destination points from parsed trails
   * This is useful for creating a separate destination collection later
   */
  static getUniqueDestinations(trails) {
    const destinationMap = new Map();
    
    trails.forEach(trail => {
      const destName = trail.puntoFine.nome;
      
      if (!destinationMap.has(destName)) {
        destinationMap.set(destName, {
          nome: destName,
          coordinate: trail.puntoFine.coordinate,
          quota: trail.puntoFine.quota,
          sentieri: []
        });
      }
      
      destinationMap.get(destName).sentieri.push(trail.codice);
    });
    
    return Array.from(destinationMap.values());
  }
}

export default KMLParser;