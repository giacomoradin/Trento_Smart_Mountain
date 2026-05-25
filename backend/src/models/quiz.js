import mongoose from "mongoose";
const { Schema, Types: { ObjectId } } = mongoose;

const quizQuestionSchema = new Schema({
  text:         { type: String, required: true, maxlength: 500 },
  choices:      {
    type: [{ type: String, maxlength: 200 }],
    validate: { validator: (v) => v.length === 4, message: "Esattamente 4 opzioni" },
  },
  correctIndex: { type: Number, required: true, min: 0, max: 3 },
  explanation:  { type: String, required: true, maxlength: 500 },
}, { _id: true });

const quizSchema = new Schema({
  categoryId:    { type: ObjectId, ref: "QuizCategory", required: true, index: true },
  title:         { type: String, required: true },
  description:   { type: String, maxlength: 500 },
  questions:     [quizQuestionSchema],
  passThreshold: { type: Number, default: 0.7 },
  creditsReward: { type: Number, default: 25, min: 0, max: 1000 },
  sortOrder:     { type: Number, default: 0 },
  createdAt:     { type: Date, default: Date.now },
});

const Quiz = mongoose.model("Quiz", quizSchema);
export default Quiz;
