/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Unauthorized Page
 * Author: Dilki
 * Description: Displays an authorization error when a user
 *              attempts to access a restricted frontend route.
 */

const UnauthorizedPage = () => {
  return (
    <div>
      <h1>403 - Unauthorized</h1>

      <p>You do not have permission to access this page.</p>
    </div>
  );
};

export default UnauthorizedPage;
