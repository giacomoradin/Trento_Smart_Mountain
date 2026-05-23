## Step 4: Run Tests

```bash
# Run all tests with coverage
npm test

# Run tests in watch mode (auto-rerun on file changes)
npm run test:watch
```

## Expected Output

If everything is set up correctly, you should see:

```
PASS  backend/__tests__/routes/auth.test.js
  Authentication Routes
    POST /auth/register/hiker
      ✓ should register a new hiker with valid data (XXXms)
      ✓ should fail with missing required fields (XXms)
      ✓ should fail with duplicate username (XXms)
      ✓ should fail with duplicate email (XXms)
      ✓ should fail with invalid email format (XXms)
      ✓ should fail with weak password (XXms)
    POST /auth/login
      ✓ should login with correct email and password (XXms)
      ✓ should fail with incorrect password (XXms)
      ✓ should fail with non-existent email (XXms)
      ... (more tests)

PASS  backend/__tests__/routes/hiker.test.js
  Hiker Routes
    GET /hikers/:id
      ✓ should return hiker profile with valid token (XXms)
      ✓ should return 401 without authorization header (XXms)
      ... (more tests)

Test Suites: 2 passed, 2 total
Tests:       23 passed, 23 total
Snapshots:   0 total
Time:        X.XXXs

Coverage summary:
---------------------|---------|----------|---------|---------|-------------------
File                 | % Stmts | % Branch | % Funcs | % Lines | Uncovered Line #s 
---------------------|---------|----------|---------|---------|-------------------
All files            |   XX.XX |    XX.XX |   XX.XX |   XX.XX |                   
 services            |   XX.XX |    XX.XX |   XX.XX |   XX.XX |                   
  authService.js     |   XX.XX |    XX.XX |   XX.XX |   XX.XX | ...               
  hikerService.js    |   XX.XX |    XX.XX |   XX.XX |   XX.XX | ...               
---------------------|---------|----------|---------|---------|-------------------
```

## ⚠️ Important Notes

### 1. Service Files Must Handle Test Cases

Your service files (`authService.js`, `hikerService.js`) must properly handle:

- **Validation errors** → return 400 with error message
- **Duplicate entries** → return 400 with "already exists" message
- **Not found** → return 404
- **Invalid credentials** → return 401
- **Unverified users** → return 401/403

### 2. Expected Response Format

**Registration (`createHiker`):**

```javascript
// Success: 201
{
  token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  user: {
    _id: "507f1f77bcf86cd799439011",
    username: "mario_rossi",
    email: "mario@example.com",
    role: "groupLeader",
    // NO passwordHash, NO password
  }
}

// Error: 400
{
  message: "Email already exists"
}
```

**Login (`loginUser`):**

```javascript
// Success: 200
{
  token: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  user: {
    _id: "507f1f77bcf86cd799439011",
    username: "mario_rossi",
    email: "mario@example.com",
    role: "groupLeader"
  }
}

// Error: 401
{
  message: "Invalid credentials"
}
```

**Get Hiker (`getHikerById`):**

```javascript
// Success: 200
{
  _id: "507f1f77bcf86cd799439011",
  username: "mario_rossi",
  email: "mario@example.com",
  role: "groupLeader",
  createdAt: "2026-05-23T10:00:00.000Z"
  // NO passwordHash, NO verificationToken
}

// Error: 404
{
  message: "User not found"
}

// Error: 400 (invalid ID)
{
  message: "Invalid user ID"
}
```

### 3. Some Tests Might Fail Initially

This is **normal**! The tests are written based on REST API best practices. You might need to adjust:

1. **Error messages**: Tests expect specific messages like "Invalid credentials", "Email already exists"
2. **Status codes**: Make sure your services return correct HTTP codes (400, 401, 404)
3. **Response format**: Ensure responses match the expected structure above
4. **Validation**: Add proper validation for email format, password strength

## 🐛 Common Issues & Fixes

### Issue 1: "Cannot find module"

**Fix**: Make sure all imports include `.js` extension:

```javascript
import User from './models/user.js'; // ✓ Correct
import User from './models/user';    // ✗ Wrong
```

### Issue 2: Tests timeout

**Fix**: Already handled in `jest.config.js` with `testTimeout: 10000`

### Issue 3: "MongoDB connection error"

**Fix**: Make sure you installed `mongodb-memory-server`:

```bash
npm install --save-dev mongodb-memory-server
```

### Issue 4: Some tests fail

**Fix**: This is expected! Adjust your service files to match the expected behavior:

- Check error messages
- Check status codes
- Check response format

## 📊 Next Steps After Tests Pass

1. **Add more test files**:
    
    - `backend/__tests__/routes/session.test.js` (HikeSession routes)
    - `backend/__tests__/routes/refuge.test.js` (Refuge routes)
    - `backend/__tests__/middleware/auth.test.js` (Middleware)
2. **Increase coverage**: Aim for >80% code coverage
    
3. **Add integration tests**: Test complete user flows
    
4. **Set up CI/CD**: Run tests automatically on git push
    

## 📖 Full Documentation

See `backend/__tests__/README.md` for complete documentation.