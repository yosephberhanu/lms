export const environment = {
  production: false,
  /** Browser-reachable Keycloak (docker-compose maps 8180:8080). */
  keycloakUrl: 'http://localhost:8180',
  realm: 'lms',
  clientId: 'lms-spa'
};
