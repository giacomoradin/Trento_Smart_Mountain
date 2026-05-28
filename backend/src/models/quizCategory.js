import mongoose from "mongoose";
const { Schema } = mongoose;

const quizCategorySchema = new Schema({
  slug:        { type: String, required: true, unique: true },
  name:        { type: String, required: true },
  description: { type: String, maxlength: 500 },
  color:       { type: String, required: true },
  sortOrder:   { type: Number, default: 0 },
  iconName:    { type: String, default: "school" },
});

const QuizCategory = mongoose.model("QuizCategory", quizCategorySchema);
export default QuizCategory;
