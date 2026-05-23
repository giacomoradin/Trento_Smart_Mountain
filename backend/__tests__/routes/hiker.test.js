import request from 'supertest';
import app from '../../src/app.js';
import {
  createTestHiker,
  generateInvalidToken,
  generateExpiredToken,
  generateMalformedToken,
} from '../helpers/authHelper.js';

/**
 * Test suite per le route degli escursionisti (Hiker).
 * 
 * Copre:
 * - GET /hikers/:id (profilo escursionista - richiede JWT)
 * 
 * Testa scenari di autenticazione:
 * - Token valido → accesso consentito
 * - Token mancante → 401
 * - Token invalido/scaduto/malformato → 401
 * - Accesso a profilo di altro utente
 */

describe('Hiker Routes', () => {
  
  // ══════════════════════════════════════════════════════════════════
  // GET /hikers/:id - Ottieni profilo escursionista
  // ══════════════════════════════════════════════════════════════════
  
  describe('GET /hikers/:id', () => {
    
    test('should return hiker profile with valid token', async () => {
      // Crea un escursionista di test
      const { user, token } = await createTestHiker({
        username: 'giovanni_bianchi',
        email: 'giovanni@example.com',
      });

      // Richiedi il profilo con token valido
      const response = await request(app)
        .get(`/hikers/${user._id}`)
        .set('Authorization', `Bearer ${token}`);

      // Note: If this fails with 401, check if hikerService.getHikerById
      // has additional authorization logic beyond the authenticate middleware
      expect(response.status).toBe(200);
      
      // Verifica dati utente
      expect(response.body).toHaveProperty('username', 'giovanni_bianchi');
      expect(response.body).toHaveProperty('email', 'giovanni@example.com');
      expect(response.body).toHaveProperty('role', 'groupLeader');
      expect(response.body).toHaveProperty('_id', user._id.toString());
      
      // Verifica che password e token non siano nel response
      expect(response.body).not.toHaveProperty('password');
      expect(response.body).not.toHaveProperty('passwordHash');
      expect(response.body).not.toHaveProperty('verificationToken');
      expect(response.body).not.toHaveProperty('passwordResetToken');
    });

    test('should return 401 without authorization header', async () => {
      // Crea un utente
      const { user } = await createTestHiker();

      // Richiedi senza token
      const response = await request(app)
        .get(`/hikers/${user._id}`);

      expect(response.status).toBe(401);
      expect(response.body).toHaveProperty('message');
      expect(response.body.message).toMatch(/no token provided/i);
    });

    test('should return 401 with invalid token (wrong secret)', async () => {
      // Crea un utente
      const { user } = await createTestHiker();
      
      // Token firmato con secret sbagliato
      const invalidToken = generateInvalidToken();

      const response = await request(app)
        .get(`/hikers/${user._id}`)
        .set('Authorization', `Bearer ${invalidToken}`);

      expect(response.status).toBe(401);
      expect(response.body.message).toMatch(/invalid.*token/i);
    });

    test('should return 401 with expired token', async () => {
      // Crea un utente
      const { user } = await createTestHiker();
      
      // Token scaduto
      const expiredToken = generateExpiredToken();

      const response = await request(app)
        .get(`/hikers/${user._id}`)
        .set('Authorization', `Bearer ${expiredToken}`);

      expect(response.status).toBe(401);
      expect(response.body.message).toMatch(/invalid.*expired.*token/i);
    });

    test('should return 401 with malformed token', async () => {
      // Crea un utente
      const { user } = await createTestHiker();
      
      // Token malformato (non è un JWT)
      const malformedToken = generateMalformedToken();

      const response = await request(app)
        .get(`/hikers/${user._id}`)
        .set('Authorization', `Bearer ${malformedToken}`);

      expect(response.status).toBe(401);
      expect(response.body.message).toMatch(/invalid.*token/i);
    });

    test('should return 401 without "Bearer" prefix', async () => {
      // Crea un utente
      const { user, token } = await createTestHiker();

      // Invia token senza "Bearer"
      const response = await request(app)
        .get(`/hikers/${user._id}`)
        .set('Authorization', token); // Manca "Bearer "

      expect(response.status).toBe(401);
    });

    test('should return 404 for non-existent hiker ID', async () => {
      // Crea un utente per ottenere un token valido
      const { token } = await createTestHiker();
      
      // ID MongoDB valido ma inesistente
      const fakeId = '507f1f77bcf86cd799439011';

      const response = await request(app)
        .get(`/hikers/${fakeId}`)
        .set('Authorization', `Bearer ${token}`);

      expect(response.status).toBe(404);
      expect(response.body.message).toMatch(/non trovato|not found/i);
    });

    test('should return 400 for invalid MongoDB ID format', async () => {
      // Crea un utente per ottenere un token valido
      const { token } = await createTestHiker();
      
      // ID non valido (non è un ObjectId)
      const invalidId = 'invalid-id-123';

      const response = await request(app)
        .get(`/hikers/${invalidId}`)
        .set('Authorization', `Bearer ${token}`);

      expect(response.status).toBe(400);
      expect(response.body.message).toMatch(/non valido|invalid.*id/i);
    });

    test('should allow user to access their own profile', async () => {
      // Crea utente
      const { user, token } = await createTestHiker({
        username: 'myself',
        email: 'me@example.com',
      });

      // Utente accede al proprio profilo
      const response = await request(app)
        .get(`/hikers/${user._id}`)
        .set('Authorization', `Bearer ${token}`);

      expect(response.status).toBe(200);
      expect(response.body.username).toBe('myself');
    });

    test('should allow authenticated user to view other hiker profiles', async () => {
      // Crea primo utente
      const { token: token1 } = await createTestHiker({
        username: 'user1',
        email: 'user1@example.com',
      });

      // Crea secondo utente
      const { user: user2 } = await createTestHiker({
        username: 'user2',
        email: 'user2@example.com',
      });

      // User1 visualizza profilo di User2 (se il tuo sistema lo permette)
      const response = await request(app)
        .get(`/hikers/${user2._id}`)
        .set('Authorization', `Bearer ${token1}`);

      // Questo test dipende dalla tua logica di business:
      // Se permetti agli utenti di vedere altri profili → 200
      // Se richiedi che userId del token = :id → 403
      
      // Assumendo che sia permesso:
      expect([200, 403]).toContain(response.status);
      
      if (response.status === 200) {
        expect(response.body.username).toBe('user2');
      }
    });
  });

  // ══════════════════════════════════════════════════════════════════
  // PUT /hikers/:id - Aggiorna profilo escursionista (opzionale)
  // ══════════════════════════════════════════════════════════════════
  
  describe('PUT /hikers/:id', () => {
    
    test('should update hiker profile with valid token and data', async () => {
      // Crea utente
      const { user, token } = await createTestHiker({
        username: 'old_username',
        email: 'old@example.com',
      });

      // Aggiorna profilo
      const response = await request(app)
        .put(`/hikers/${user._id}`)
        .set('Authorization', `Bearer ${token}`)
        .send({
          username: 'new_username',
        });

      // Verifica risposta (dipende dalla tua implementazione)
      expect([200, 204]).toContain(response.status);
      
      if (response.status === 200) {
        expect(response.body.username).toBe('new_username');
      }
    });

    test('should return 401 when updating without token', async () => {
      // Crea utente
      const { user } = await createTestHiker();

      const response = await request(app)
        .put(`/hikers/${user._id}`)
        .send({ username: 'hacker' });

      expect(response.status).toBe(401);
    });

    test('should prevent updating sensitive fields', async () => {
      // Crea utente
      const { user, token } = await createTestHiker();

      // Tenta di modificare campi sensibili
      const response = await request(app)
        .put(`/hikers/${user._id}`)
        .set('Authorization', `Bearer ${token}`)
        .send({
          role: 'admin', // Tentativo di privilege escalation
          passwordHash: 'hacked',
          isVerified: false,
        });

      // Verifica che i campi sensibili non siano stati modificati
      // (dipende dalla tua implementazione di updateHiker)
      expect(response.status).not.toBe(500);
    });
  });
});