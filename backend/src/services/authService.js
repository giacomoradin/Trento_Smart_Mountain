import User from "../models/user.js";
import bcrypt from "bcrypt";
import jwt from "jsonwebtoken";

export const loginUser = async (req, res) => {
  try {
    const { email, password } = req.body;

    //find user by email
    const user = await User.findOne({ email });

    if (!user) {
      return res.status(401).json({ message: "Invalid email" });
    }

    //compare provided password with stored hash
    const isPasswordValid = await bcrypt.compare(password, user.passwordHash);

    if (!isPasswordValid) {
      return res.status(401).json({ message: "password" });
    }

    //generate JWT token
    const token = jwt.sign(
      { userId: user._id, role: user.role },
      process.env.JWT_SECRET,
      // token expires in 1 day by default, can be configured via .env with JWT_EXPIRES_IN variable
      { expiresIn: process.env.JWT_EXPIRES_IN || "1d" },
    );

    res.status(200).json({ token }); //I used https://jwt.io to decode the token
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};
