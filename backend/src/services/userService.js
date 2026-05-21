export const createUser = async (req, res) => {
  
  try {
    const { username, email, password, role, rifugioDetails } = req.body;

    const passwordHash = await bcrypt.hash(password, 10);

    const verificationToken = crypto.randomBytes(32).toString("hex");
    const user = new User({
      username,
      email,
      passwordHash,
      role,
      isVerified: false,
      verificationToken,
      ...(rifugioDetails && { rifugioDetails }),
    });

    const savedUser = await user.save();

    // ✅ INVIO EMAIL ASINCRONO (senza await, senza try-catch)
    sendVerificationEmail(email, verificationToken)
      .catch(emailError => {
        console.error("❌ Fallimento Transport SMTP:", emailError);
        // TODO: implementare sistema di retry
      });

    const {
      passwordHash: _,
      verificationToken: __,
      __v: ___,
      ...userWithoutPassword
    } = savedUser.toObject();
    
    res.status(201).json({
      message: "Allocazione completata. Attesa verifica email.",
      user: userWithoutPassword,
    });
  } catch (error) {
    if (error.code === 11000)
      return res.status(409).json({
        message: "Collisione rilevata: Email o Username già utilizzati.",
      });
    res.status(500).json({ message: error.message });
  }
};