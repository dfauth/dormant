# Use a lightweight official Node.js image
FROM node:22-alpine

# Set the working directory inside the container
WORKDIR /usr/src/app

# Copy package files first to leverage Docker build cache
COPY package*.json ./

# Install only production dependencies for a smaller image
RUN npm ci --omit=dev

# Copy the rest of the application source code
COPY . .

# Expose the port your app runs on
EXPOSE 3000

# Run the app securely as a non-root user
USER node

# Execute the application directly using node
CMD ["node", "index.js"]
