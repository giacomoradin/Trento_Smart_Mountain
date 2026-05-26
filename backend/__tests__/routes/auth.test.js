import request from 'supertest';
import app from '../../src/app.js';
import Hiker from '../../src/models/hiker.js';
import { createTestHiker } from '../helpers/authHelper.js';

/**
 * Test suite per le route di autenticazione.
 * 
 * Copre:
 * - POST /auth/register/hiker (registrazione escursionista)
 * - POST /auth/login (login con email/password)
 */

describe('Authentication Routes', () => {
  
  // ══════════════════════════════════════════════════════════════════
  // POST /auth/register/hiker - Registrazione Escursionista
  // ══════════════════════════════════════════════════════════════════
  
  describe('POST /auth/register/hiker', () => {
    
    test('should register a new hiker with valid data', async () => {
      const newHiker = {
        username: 'mariorossi',
        email: 'mario.rossi@example.com',
        password: 'SecurePassword123!',
      };

      const response = await request(app)
        .post('/auth/register/hiker')
        .send(newHiker);

      // Verifica status code
      expect(response.status).toBe(201);
      
      // Verifica messaggio di conferma (NO token until email verified)
      expect(response.body).toHaveProperty('message');
      expect(response.body.message).toMatch(/verifica.*email/i);
      
      // Verifica che i dati utente siano corretti
      expect(response.body.user).toHaveProperty('username', 'mariorossi');
      expect(response.body.user).toHaveProperty('email', 'mario.rossi@example.com');
      expect(response.body.user).toHaveProperty('role', 'groupLeader');
      expect(response.body.user).toHaveProperty('isVerified', false);
      
      // Verifica che la password NON sia nel response
      expect(response.body.user).not.toHaveProperty('password');
      expect(response.body.user).not.toHaveProperty('passwordHash');
      expect(response.body.user).not.toHaveProperty('verificationToken');

      // Verifica che l'utente sia stato salvato nel database
      const savedUser = await Hiker.findOne({ email: 'mario.rossi@example.com' });
      expect(savedUser).toBeTruthy();
      expect(savedUser.username).toBe('mariorossi');
      expect(savedUser.role).toBe('groupLeader');
      expect(savedUser.isVerified).toBe(false);
    });

    test('should fail with missing required fields', async () => {
      const incompleteData = {
        username: 'onlyusername',
        // mancano email e password
      };

      const response = await request(app)
        .post('/auth/register/hiker')
        .send(incompleteData);

      expect(response.status).toBe(422);
      expect(response.body).toHaveProperty('error');
    });

    test('should fail with duplicate username', async () => {
      // Crea primo utente
      await createTestHiker({
        username: 'duplicateuser',
        email: 'first@example.com',
      });

      // Tenta di registrare con stesso username
      const response = await request(app)
        .post('/auth/register/hiker')
        .send({
          username: 'duplicateuser',
          email: 'second@example.com',
          password: 'Password123!',
        });

      expect(response.status).toBe(409); // Conflict
      expect(response.body.message).toMatch(/username.*già|email.*username.*registrat|already.*exists/i);
    });

    test('should fail with duplicate email', async () => {
      // Crea primo utente
      await createTestHiker({
        username: 'user1',
        email: 'duplicate@example.com',
      });

      // Tenta di registrare con stessa email
      const response = await request(app)
        .post('/auth/register/hiker')
        .send({
          username: 'user2',
          email: 'duplicate@example.com',
          password: 'Password123!',
        });

      expect(response.status).toBe(409); // Conflict
      expect(response.body.message).toMatch(/email.*già|email.*username.*registrat|already.*exists/i);
    });

    // Joi `.email()` reject malformed addresses con 422 (validate middleware).
    test('should fail with invalid email format', async () => {
      const response = await request(app)
        .post('/auth/register/hiker')
        .send({
          username: 'testuser',
          email: 'invalid-email-format',
          password: 'Password123!',
        });

      expect(response.status).toBe(422);
      // L'errore Joi viene wrappato dal middleware in { error, details: [{ path, message }] }
      expect(response.body.details).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ path: 'email' }),
        ]),
      );
    });

    test('should fail with weak password', async () => {
      const response = await request(app)
        .post('/auth/register/hiker')
        .send({
          username: 'testuser',
          email: 'test@example.com',
          password: '123', // Password troppo debole
        });

      expect(response.status).toBe(422);
      expect(response.body.details[0].path).toBe('password');
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // POST /auth/login - Login
  // ══════════════════════════════════════════════════════════════════
  
  describe('POST /auth/login', () => {
    
    test('should login with correct email and password', async () => {
      // Crea un utente di test
      const { user, password } = await createTestHiker({
        username: 'logintest',
        email: 'login@example.com',
      });

      const response = await request(app)
        .post('/auth/login')
        .send({
          email: 'login@example.com',
          password: password, // Password in chiaro dal helper
        });

      // Verifica status code
      expect(response.status).toBe(200);
      
      // Verifica token JWT (API returns ONLY token, no user object)
      expect(response.body).toHaveProperty('token');
      expect(typeof response.body.token).toBe('string');
      expect(response.body.token.length).toBeGreaterThan(20);
      
      // Verifica che la password non sia nel response
      expect(response.body).not.toHaveProperty('password');
      expect(response.body).not.toHaveProperty('passwordHash');
    });

    test('should fail with incorrect password', async () => {
      // Crea utente
      await createTestHiker({
        email: 'wrongpass@example.com',
      });

      const response = await request(app)
        .post('/auth/login')
        .send({
          email: 'wrongpass@example.com',
          password: 'WrongPassword123!',
        });

      expect(response.status).toBe(401);
      expect(response.body).toHaveProperty('message');
      expect(response.body.message).toMatch(/credenziali.*non.*valide|invalid.*credentials/i);
    });

    test('should fail with non-existent email', async () => {
      const response = await request(app)
        .post('/auth/login')
        .send({
          email: 'nonexistent@example.com',
          password: 'SomePassword123!',
        });

      expect(response.status).toBe(401);
      expect(response.body.message).toMatch(/credenziali.*non.*valide|invalid.*credentials/i);
    });

    test('should fail with missing email', async () => {
      const response = await request(app)
        .post('/auth/login')
        .send({
          password: 'Password123!',
        });

      expect(response.status).toBe(422);
      expect(response.body).toHaveProperty('error');
    });

    test('should fail with missing password', async () => {
      const response = await request(app)
        .post('/auth/login')
        .send({
          email: 'test@example.com',
        });

      expect(response.status).toBe(422);
      expect(response.body).toHaveProperty('error');
    });

    test('should fail for unverified user', async () => {
      // Crea utente non verificato
      const { password } = await createTestHiker({
        email: 'unverified@example.com',
      });
      
      // Imposta isVerified a false
      await Hiker.findOneAndUpdate(
        { email: 'unverified@example.com' },
        { isVerified: false }
      );

      const response = await request(app)
        .post('/auth/login')
        .send({
          email: 'unverified@example.com',
          password: password,
        });

      expect(response.status).toBe(403);
      expect(response.body.message).toMatch(/verifica|verified/i);
    });
  });
});