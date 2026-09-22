/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Coming Soon Page
 * Author: Dilki
 * Description: Temporary placeholder used while individual
 *              application modules are being implemented.
 */

interface ComingSoonPageProps {
  title: string;
}

const ComingSoonPage = ({ title }: ComingSoonPageProps) => {
  return (
    <div className="dashboard-header">
      <h1>{title}</h1>

      <p>This module is currently being implemented.</p>
    </div>
  );
};

export default ComingSoonPage;
