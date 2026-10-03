FROM node:20-slim

WORKDIR app

# 1. Change ownership of the working directory to the node user
RUN chown -R node:node /app

# 2. Tell Docker to execute subsequent instructions as 'node'
USER node

# 3. Copy package files and install dependencies as the 'node' user
COPY --chown=node:node package*.json ./
RUN npm install

# 4. Copy the remaining application files
COPY --chown=node:node . .

# 5. Build and expose
RUN npm run build
EXPOSE 4173

CMD ["npm", "start"]
