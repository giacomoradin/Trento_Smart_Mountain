import User from "../models/user.js";
import bcrypt from "bcrypt";
import crypto from "crypto";
import { sendVerificationEmail } from "./emailService.js";

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

    // ✅ INVIO EMAIL ASINCRONO (non blocca la risposta)
    sendVerificationEmail(email, verificationToken)
      .catch(emailError => {
        console.error("❌ Fallimento Transport SMTP:", emailError);
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

export const getAllUsers = async (req, res) => {
  try {
    const users = await User.find().select("-passwordHash -__v");
    res.status(200).json(users);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

export const getUserById = async (req, res) => {
  try {
    const user = await User.findById(req.params.id).select("-passwordHash -__v");

    if (!user) {
      return res.status(404).json({ message: "User not found." });
    }

    res.status(200).json(user);
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "Invalid user ID format." });
    }
    res.status(500).json({ message: error.message });
  }
};

export const updateUser = async (req, res) => {
  try {
    const allowedUpdates = [
      "username",
      "email",
      "passwordHash",
      "role",
      "sessionRoles",
    ];
    const updates = {};

    for (const key of allowedUpdates) {
      if (req.body[key] !== undefined) {
        updates[key] = req.body[key];
      }
    }

    const updatedUser = await User.findByIdAndUpdate(req.params.id, updates, {
      new: true,
      runValidators: true,
    }).select("-passwordHash -__v");

    if (!updatedUser) {
      return res.status(404).json({ message: "User not found." });
    }

    res.status(200).json(updatedUser);
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "Invalid user ID format." });
    }
    if (error.code === 11000) {
      return res
        .status(409)
        .json({ message: "Username or email already in use." });
    }
    res.status(500).json({ message: error.message });
  }
};

export const deleteUser = async (req, res) => {
  try {
    const deletedUser = await User.findByIdAndDelete(req.params.id);

    if (!deletedUser) {
      return res.status(404).json({ message: "User not found." });
    }

    res.status(200).json({ message: "User deleted successfully." });
  } catch (error) {
    if (error.name === "CastError") {
      return res.status(400).json({ message: "Invalid user ID format." });
    }
    res.status(500).json({ message: error.message });
  }
};