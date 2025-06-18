// eslint.config.js
/** @type {import('eslint').Linter.Config} */
const config = [
  {
    languageOptions: {
      globals: {
        process: "readonly",
        require: "readonly"
      },
      parserOptions: {
        ecmaVersion: 2021
      }
    },
    rules: {
      quotes: ["error", "double"],
      indent: ["error", 2],
      "object-curly-spacing": ["error", "never"],
      "comma-dangle": ["error", "never"],
      "max-len": ["error", {code: 80}],
      "arrow-parens": ["error", "as-needed"],
      "no-unused-vars": ["warn"]
    }
  }
];

module.exports = config;