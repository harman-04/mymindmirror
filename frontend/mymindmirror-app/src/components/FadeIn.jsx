import React from 'react';
import { motion } from 'framer-motion';

const FadeIn = ({ children, delay = 0, direction = 'up', className = '', fullWidth = false }) => {
  const directions = {
    up: { y: 30, x: 0 },
    down: { y: -30, x: 0 },
    left: { x: 30, y: 0 },
    right: { x: -30, y: 0 },
    none: { x: 0, y: 0 }
  };

  return (
      <motion.div
        initial={{ opacity: 0, ...directions[direction] }}
        whileInView={{ opacity: 1, x: 0, y: 0 }}
        // 💡 THE FIX: Removed 'amount'. Added a 50px margin so it triggers instantly
        // when it approaches the viewport, no matter how tall the container is.
        viewport={{ once: true, margin: "50px" }}
        transition={{ duration: 0.4, delay: delay, ease: "easeOut" }}
        className={`${fullWidth ? 'w-full' : ''} ${className}`}
      >
        {children}
      </motion.div>
    );
};

export default FadeIn;