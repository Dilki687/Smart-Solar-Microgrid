import { useEffect, useState, type FormEvent } from "react";
import type {
  CreateUserRequest,
  UpdateUserRequest,
  User,
  UserRole,
} from "../types/user";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: User Form Modal
 * Author: Dilki
 * Description: Reusable modal used to create and edit
 *              Backoffice and Grid Operator accounts.
 */

interface UserFormModalProps {
  mode: "create" | "edit";
  user?: User | null;
  onClose: () => void;
  onSubmit: (data: CreateUserRequest | UpdateUserRequest) => Promise<void>;
}

const UserFormModal = ({
  mode,
  user,
  onClose,
  onSubmit,
}: UserFormModalProps) => {
  const [nic, setNic] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<"BACKOFFICE" | "GRID_OPERATOR">(
    "GRID_OPERATOR",
  );

  const [submitting, setSubmitting] = useState(false);

  const [error, setError] = useState("");

  /**
   * Populate the form when editing an existing user.
   */
  useEffect(() => {
    if (mode === "edit" && user) {
      setNic(user.nic);
      setName(user.name);
      setEmail(user.email);
      setPhone(user.phone);
      setAddress(user.address);
      setRole(user.role as "BACKOFFICE" | "GRID_OPERATOR");

      // Password is intentionally empty during editing.
      setPassword("");
    } else {
      // Reset the form for creating a new user.
      setNic("");
      setName("");
      setEmail("");
      setPhone("");
      setAddress("");
      setPassword("");
      setRole("GRID_OPERATOR");
    }

    setError("");
  }, [mode, user]);

  /**
   * Submit the form.
   */
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setError("");

    // Validate common required fields.
    if (
      !nic.trim() ||
      !name.trim() ||
      !email.trim() ||
      !phone.trim() ||
      !address.trim()
    ) {
      setError("Please fill in all required fields.");

      return;
    }

    // Password is required only when creating a user.
    if (mode === "create" && !password.trim()) {
      setError("Password is required when creating a user.");

      return;
    }

    try {
      setSubmitting(true);

      if (mode === "create") {
        const createRequest: CreateUserRequest = {
          nic: nic.trim(),
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          address: address.trim(),
          password: password,
          role,
        };

        await onSubmit(createRequest);
      } else {
        const updateRequest: UpdateUserRequest = {
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          address: address.trim(),
          role,
        };

        await onSubmit(updateRequest);
      }
    } catch (err) {
      console.error("Failed to submit user form:", err);

      const backendError = (
        err as {
          response?: {
            data?: {
              message?: string;
              error?: string;
              title?: string;
            };
          };
        }
      )?.response?.data;

      setError(
        backendError?.message ||
          backendError?.error ||
          backendError?.title ||
          "Unable to save the user. Please try again.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const isEditing = mode === "edit";

  return (
    <div
      className="modal-overlay"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !submitting) {
          onClose();
        }
      }}
    >
      <div className="modal-container">
        {/* Modal header */}
        <div className="modal-header">
          <div>
            <h2>{isEditing ? "Edit User" : "Add User"}</h2>

            <p>
              {isEditing
                ? "Update the selected user account."
                : "Create a new system user account."}
            </p>
          </div>

          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            disabled={submitting}
            aria-label="Close"
          >
            ×
          </button>
        </div>

        {/* Error */}
        {error && <div className="alert alert-error modal-alert">{error}</div>}

        <form className="user-form" onSubmit={handleSubmit}>
          {/* NIC */}
          <div className="form-group">
            <label htmlFor="user-nic">NIC</label>

            <input
              id="user-nic"
              type="text"
              value={nic}
              onChange={(event) => setNic(event.target.value)}
              placeholder="Enter NIC"
              disabled={submitting || isEditing}
            />

            {isEditing && (
              <small className="form-help">NIC cannot be changed.</small>
            )}
          </div>

          {/* Name */}
          <div className="form-group">
            <label htmlFor="user-name">Full Name</label>

            <input
              id="user-name"
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Enter full name"
              disabled={submitting}
            />
          </div>

          {/* Email */}
          <div className="form-group">
            <label htmlFor="user-email">Email</label>

            <input
              id="user-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="Enter email address"
              disabled={submitting}
            />
          </div>

          {/* Phone */}
          <div className="form-group">
            <label htmlFor="user-phone">Phone</label>

            <input
              id="user-phone"
              type="tel"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="Enter phone number"
              disabled={submitting}
            />
          </div>

          {/* Address */}
          <div className="form-group">
            <label htmlFor="user-address">Address</label>

            <textarea
              id="user-address"
              value={address}
              onChange={(event) => setAddress(event.target.value)}
              placeholder="Enter address"
              rows={3}
              disabled={submitting}
            />
          </div>

          {/* Password */}
          {!isEditing && (
            <div className="form-group">
              <label htmlFor="user-password">Password</label>

              <input
                id="user-password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter temporary password"
                disabled={submitting}
              />
            </div>
          )}

          {/* Role */}
          <div className="form-group">
            <label htmlFor="user-role">Role</label>

            <select
              id="user-role"
              value={role}
              onChange={(event) =>
                setRole(event.target.value as "BACKOFFICE" | "GRID_OPERATOR")
              }
              disabled={submitting}
            >
              <option value="GRID_OPERATOR">Grid Operator</option>

              <option value="BACKOFFICE">Backoffice</option>
            </select>
          </div>

          {/* Form buttons */}
          <div className="form-actions">
            <button
              type="button"
              className="secondary-button"
              onClick={onClose}
              disabled={submitting}
            >
              Cancel
            </button>

            <button
              type="submit"
              className="primary-button"
              disabled={submitting}
            >
              {submitting
                ? "Saving..."
                : isEditing
                  ? "Save Changes"
                  : "Create User"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UserFormModal;
