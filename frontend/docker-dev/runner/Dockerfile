# => Run container
FROM nginx:1.15.2-alpine

# Add bash
RUN apk add --no-cache bash

# Default port exposure
EXPOSE 80

# Copy .env file and shell script to container
WORKDIR /usr/share/nginx/html

# Start Nginx server
CMD ["nginx", "-g", "daemon off;"]