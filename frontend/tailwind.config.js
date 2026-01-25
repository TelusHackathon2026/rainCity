
export default {
  content: ["./src//*.{js,jsx,ts,tsx}"],
  theme: {
    extend: {
      animation: {
        "fade-in-down": "fadeInDown 0.4s ease-out forwards",
      },
      keyframes: {
        fadeInDown: {
          "0%": { opacity: "0", transform: "translateY(-3px)" },
          "100%": { opacity: "1", transform: "translateY(0)" },
        },
      },
    },
  },
  plugins: [],
};

