import User from "./models/user.js";
import bcrypt from "bcrypt";

export const createUser = async (req, res) => {
  try {
    const { username, email, password, role } = req.body; // plain password from client

    const passwordHash = await bcrypt.hash(password, 10); // server hashes it

    const user = new User({ username, email, passwordHash, role });
    const savedUser = await user.save();
    // exclude passwordHash from response
    //passwordHsh renamend to _ and ... rest operator to get the rest of the user fields without passwordHash
    const {
      passwordHash: _,
      __v: __,
      ...userWithoutPassword
    } = savedUser.toObject();

    res.status(201).json(userWithoutPassword);
  } catch (error) {
    if (error.code === 11000) {
      return res
        .status(409)
        .json({ message: "Username or email already in use." });
    }
    res.status(500).json({ message: error.message });
  }
};

// GET /users — Get all users
export const getAllUsers = async (req, res) => {
  try {
    // Exclude passwordHash from results — never expose it
    const users = await User.find().select("-passwordHash -__v");

    res.status(200).json(users);
  } catch (error) {
    res.status(500).json({ message: error.message });
  }
};

// GET /users/:id — Get a single user by ID
export const getUserById = async (req, res) => {
  try {
    //https://mongoosejs.com/docs/api/query.html#Query.prototype.select()
    //exclude not wanted fields
    const user = await User.findById(req.params.id).select(
      "-passwordHash -__v",
    );

    if (!user) {
      return res.status(404).json({ message: "User not found." });
    }

    res.status(200).json(user);
  } catch (error) {
    // Malformed MongoDB ObjectId
    if (error.name === "CastError") {
      return res.status(400).json({ message: "Invalid user ID format." });
    }
    res.status(500).json({ message: error.message });
  }
};

// PUT /users/:id — Update a user by ID
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

    // Only pick fields that are actually allowed to be updated
    for (const key of allowedUpdates) {
      if (req.body[key] !== undefined) {
        updates[key] = req.body[key];
      }
    }

    const updatedUser = await User.findByIdAndUpdate(req.params.id, updates, {
      new: true, // return the updated document
      runValidators: true, // enforce schema rules on update
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

// DELETE /users/:id — Delete a user by ID
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
