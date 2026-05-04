FROM node:20-alpine AS build

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts

COPY . .

RUN npm run build -- --configuration production

FROM nginx:alpine AS runtime

COPY docker-nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/admin-app/browser /usr/share/nginx/html

EXPOSE 80
