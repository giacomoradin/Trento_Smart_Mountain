import mongoose from "mongoose";
const { Schema, Types: { ObjectId } } = mongoose;

const quizAttemptSchema = new Schema({
  userId:         { type: ObjectId, ref: "User", required: true, index: true },
  quizId:         { type: ObjectId, ref: "Quiz", required: true, index: true },
  answers:        [{ questionId: ObjectId, choiceIndex: Number }],
  correctCount:   { type: Number, required: true },
  totalQuestions: { type: Number, required: true },
  passed:         { type: Boolean, required: true },
  creditsAwarded: { type: Number, default: 0 },
  createdAt:      { type: Date, default: Date.now },
});
quizAttemptSchema.index({ userId: 1, quizId: 1, passed: 1 });

const QuizAttempt = mongoose.model("QuizAttempt", quizAttemptSchema);
export default QuizAttempt;
