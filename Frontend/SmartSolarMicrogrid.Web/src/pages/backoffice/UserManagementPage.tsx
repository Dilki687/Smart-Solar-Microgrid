import { useEffect, useMemo, useState } from "react";
import {
  createUser,
  deactivateUser,
  getUsers,
  updateUser,
} from "../../services/userService";
import type {
  CreateUserRequest,
  UpdateUserRequest,
  User,
} from "../../types/user";
import UserFormModal from "../../components/UserFormModal";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: User Management Page
 * Author: Dilki
 * Description: Provides Backoffice functionality for viewing,
 *              creating, editing, filtering and deactivating
 *              system users.
 */

const UserManagementPage = () => {
  // Store all users returned by the backend.
  const [users, setUsers] = useState<User[]>([]);

  // Loading state for the initial user request.
  const [loading, setLoading] = useState(true);

  // Operation loading state.
  const [actionLoading, setActionLoading] = useState(false);

  // General page error.
  const [error, setError] = useState("");

  // Success message.
  const [successMessage, setSuccessMessage] = useState("");

  // Controls the Add/Edit modal.
  const [showUserModal, setShowUserModal] = useState(false);

  // Stores the user currently being edited.
  const [selectedUser, setSelectedUser] = useState<User | null>(null);

  // Search field.
  const [searchTerm, setSearchTerm] = useState("");

  // Role filter.
  const [roleFilter, setRoleFilter] = useState("ALL");

  // Status filter.
  const [statusFilter, setStatusFilter] = useState("ALL");

  /**
   * Extract a useful message from an Axios/API error.
   */
  const getErrorMessage = (err: unknown, fallback: string): string => {
    const error = err as {
      response?: {
        data?: {
          message?: string;
          error?: string;
          title?: string;
        };
      };
      message?: string;
    };

    return (
      error.response?.data?.message ||
      error.response?.data?.error ||
      error.response?.data?.title ||
      error.message ||
      fallback
    );
  };

  /**
   * Load users from the backend.
   */
  const loadUsers = async () => {
    try {
      setError("");
      setLoading(true);

      const data = await getUsers();

      setUsers(data);
    } catch (err) {
      console.error("Failed to load users:", err);

      setError(getErrorMessage(err, "Unable to load users. Please try again."));
    } finally {
      setLoading(false);
    }
  };

  /**
   * Load users when the page opens.
   */
  useEffect(() => {
    loadUsers();
  }, []);

  /**
   * Clear notifications.
   */
  const clearMessages = () => {
    setError("");
    setSuccessMessage("");
  };

  /**
   * Open the Add User modal.
   */
  const handleAddUser = () => {
    clearMessages();

    setSelectedUser(null);
    setShowUserModal(true);
  };

  /**
   * Open the Edit User modal.
   */
  const handleEditUser = (user: User) => {
    clearMessages();

    setSelectedUser(user);
    setShowUserModal(true);
  };

  /**
   * Close the Add/Edit modal.
   */
  const handleCloseModal = () => {
    if (!actionLoading) {
      setShowUserModal(false);
      setSelectedUser(null);
    }
  };

  /**
   * Create or update a user.
   */
  const handleSaveUser = async (
    formData: CreateUserRequest | UpdateUserRequest,
  ) => {
    try {
      clearMessages();

      setActionLoading(true);

      if (selectedUser) {
        // Update existing user.
        await updateUser(selectedUser.userId, formData as UpdateUserRequest);

        setSuccessMessage("User updated successfully.");
      } else {
        // Create new user.
        await createUser(formData as CreateUserRequest);

        setSuccessMessage("User created successfully.");
      }

      // Close modal.
      setShowUserModal(false);
      setSelectedUser(null);

      // Refresh users.
      await loadUsers();
    } catch (err) {
      console.error("Failed to save user:", err);

      setError(
        getErrorMessage(err, "Unable to save the user. Please try again."),
      );
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Deactivate a selected user.
   */
  const handleDeactivateUser = async (user: User) => {
    const confirmed = window.confirm(
      `Are you sure you want to deactivate ${user.name}?`,
    );

    if (!confirmed) {
      return;
    }

    try {
      clearMessages();

      setActionLoading(true);

      await deactivateUser(user.userId);

      setSuccessMessage(`${user.name} has been deactivated successfully.`);

      // Refresh the table.
      await loadUsers();
    } catch (err) {
      console.error("Failed to deactivate user:", err);

      setError(
        getErrorMessage(
          err,
          "Unable to deactivate the user. Please try again.",
        ),
      );
    } finally {
      setActionLoading(false);
    }
  };

  /**
   * Reset all filters.
   */
  const handleClearFilters = () => {
    setSearchTerm("");
    setRoleFilter("ALL");
    setStatusFilter("ALL");
  };

  /**
   * Apply search, role and status filters.
   */
  const filteredUsers = useMemo(() => {
    return users.filter((user) => {
      const search = searchTerm.trim().toLowerCase();

      const matchesSearch =
        !search ||
        user.nic.toLowerCase().includes(search) ||
        user.name.toLowerCase().includes(search) ||
        user.email.toLowerCase().includes(search);

      const matchesRole = roleFilter === "ALL" || user.role === roleFilter;

      const matchesStatus =
        statusFilter === "ALL" || user.status === statusFilter;

      return matchesSearch && matchesRole && matchesStatus;
    });
  }, [users, searchTerm, roleFilter, statusFilter]);

  /**
   * Display loading screen.
   */
  if (loading) {
    return (
      <div className="page-container">
        <div className="page-header">
          <div>
            <h1>User Management</h1>

            <p>Manage Backoffice and Grid Operator accounts.</p>
          </div>
        </div>

        <div className="content-card">
          <div className="loading-state">Loading users...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="page-container">
      {/* Page header */}
      <div className="page-header">
        <div>
          <h1>User Management</h1>

          <p>Manage Backoffice and Grid Operator accounts.</p>
        </div>

        <button
          type="button"
          className="primary-button"
          onClick={handleAddUser}
          disabled={actionLoading}
        >
          + Add User
        </button>
      </div>

      {/* Error message */}
      {error && (
        <div className="alert alert-error">
          <span>{error}</span>

          <button type="button" onClick={loadUsers} className="retry-button">
            Retry
          </button>
        </div>
      )}

      {/* Success message */}
      {successMessage && (
        <div className="alert alert-success">
          <span>{successMessage}</span>

          <button
            type="button"
            className="message-close-button"
            onClick={() => setSuccessMessage("")}
          >
            ×
          </button>
        </div>
      )}

      {/* Filters */}
      <div className="content-card filter-card">
        <div className="filter-header">
          <div>
            <h2>Search & Filters</h2>

            <p>Filter users by name, NIC, email, role or status.</p>
          </div>

          <button
            type="button"
            className="secondary-button"
            onClick={handleClearFilters}
          >
            Clear Filters
          </button>
        </div>

        <div className="filters-grid">
          {/* Search */}
          <div className="form-group">
            <label htmlFor="user-search">Search</label>

            <input
              id="user-search"
              type="text"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              placeholder="Search by NIC, name or email"
            />
          </div>

          {/* Role */}
          <div className="form-group">
            <label htmlFor="role-filter">Role</label>

            <select
              id="role-filter"
              value={roleFilter}
              onChange={(event) => setRoleFilter(event.target.value)}
            >
              <option value="ALL">All Roles</option>

              <option value="BACKOFFICE">Backoffice</option>

              <option value="GRID_OPERATOR">Grid Operator</option>
            </select>
          </div>

          {/* Status */}
          <div className="form-group">
            <label htmlFor="status-filter">Status</label>

            <select
              id="status-filter"
              value={statusFilter}
              onChange={(event) => setStatusFilter(event.target.value)}
            >
              <option value="ALL">All Statuses</option>

              <option value="ACTIVE">Active</option>

              <option value="INACTIVE">Inactive</option>
            </select>
          </div>
        </div>
      </div>

      {/* User table */}
      <div className="content-card user-table-card">
        <div className="card-header">
          <div>
            <h2>System Users</h2>

            <p>
              Showing {filteredUsers.length} of {users.length} users
            </p>
          </div>

          <button
            type="button"
            className="secondary-button"
            onClick={loadUsers}
            disabled={actionLoading}
          >
            Refresh
          </button>
        </div>

        {filteredUsers.length === 0 ? (
          <div className="empty-state">
            <h3>No users found</h3>

            <p>No users match the current search and filter criteria.</p>
          </div>
        ) : (
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>NIC</th>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Phone</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.userId}>
                    <td>{user.nic}</td>

                    <td>
                      <strong>{user.name}</strong>
                    </td>

                    <td>{user.email}</td>

                    <td>{user.phone || "—"}</td>

                    <td>
                      <span className={`role-badge ${user.role.toLowerCase()}`}>
                        {user.role === "GRID_OPERATOR"
                          ? "Grid Operator"
                          : "Backoffice"}
                      </span>
                    </td>

                    <td>
                      <span
                        className={`status-badge ${user.status.toLowerCase()}`}
                      >
                        {user.status === "ACTIVE" ? "Active" : "Inactive"}
                      </span>
                    </td>

                    <td>
                      <div className="table-actions">
                        <button
                          type="button"
                          className="secondary-button"
                          onClick={() => handleEditUser(user)}
                          disabled={actionLoading}
                        >
                          Edit
                        </button>

                        {user.status === "ACTIVE" && (
                          <button
                            type="button"
                            className="danger-button"
                            onClick={() => handleDeactivateUser(user)}
                            disabled={actionLoading}
                          >
                            Deactivate
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add/Edit modal */}
      {showUserModal && (
        <UserFormModal
          mode={selectedUser ? "edit" : "create"}
          user={selectedUser}
          onClose={handleCloseModal}
          onSubmit={handleSaveUser}
        />
      )}
    </div>
  );
};

export default UserManagementPage;
