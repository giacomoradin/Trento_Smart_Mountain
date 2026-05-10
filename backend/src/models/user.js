import mongoose from "mongoose";
const { Schema } = mongoose;

const userSchema = new Schema({
  username: { type: String, required: true, unique: true },
  email: { type: String, required: true, unique: true },
  passwordHash: { type: String, required: true },
  role: { type: String, enum: ["user", "admin"], default: "user" },
  // Contextual roles (session/group specific) under contruction still...
  sessionRoles: [{
    groupId: { type: Schema.Types.ObjectId, required: true },
    role: { type: String, enum: ["hiker", "groupLeader"],required: true }, // or "hiker"
    assignedAt: { type: Date, default: Date.now },
    createdBy: { type: Schema.Types.ObjectId, ref: "User" } // who assigned this role
  }],
  createdAt: { type: Date, default: Date.now },
});

const User = mongoose.model("User", userSchema);
export default User;