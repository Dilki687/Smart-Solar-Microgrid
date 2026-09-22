import { useEffect, useState, type FormEvent } from "react";
import type {
  CreateProsumerRequest,
  Prosumer,
  UpdateProsumerRequest,
} from "../types/user";

/*
 * Smart Solar Microgrid Trading System
 * Module: Web Frontend
 * Component: Prosumer Form Modal
 * Author: Dilki
 * Description: Reusable form for registering and editing
 *              prosumer accounts.
 */

interface ProsumerFormModalProps {
  mode: "create" | "edit";
  prosumer?: Prosumer | null;
  onClose: () => void;
  onSubmit: (
    data: CreateProsumerRequest | UpdateProsumerRequest,
  ) => Promise<void>;
}

const ProsumerFormModal = ({
  mode,
  prosumer,
  onClose,
  onSubmit,
}: ProsumerFormModalProps) => {
  const [nic, setNic] = useState("");
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [address, setAddress] = useState("");
  const [password, setPassword] = useState("");

  const [submitting, setSubmitting] = useState(false);

  const [error, setError] = useState("");

  const isEditing = mode === "edit";

  /**
   * Populate the form when editing.
   */
  useEffect(() => {
    if (isEditing && prosumer) {
      setNic(prosumer.nic);
      setName(prosumer.name);
      setEmail(prosumer.email);
      setPhone(prosumer.phone);
      setAddress(prosumer.address);
      setPassword("");
    } else {
      setNic("");
      setName("");
      setEmail("");
      setPhone("");
      setAddress("");
      setPassword("");
    }

    setError("");
  }, [isEditing, prosumer]);

  /**
   * Submit the prosumer form.
   */
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    setError("");

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

    if (!isEditing && !password.trim()) {
      setError("Password is required when registering a prosumer.");

      return;
    }

    try {
      setSubmitting(true);

      if (isEditing) {
        const request: UpdateProsumerRequest = {
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          address: address.trim(),
        };

        await onSubmit(request);
      } else {
        const request: CreateProsumerRequest = {
          nic: nic.trim(),
          name: name.trim(),
          email: email.trim(),
          phone: phone.trim(),
          address: address.trim(),
          password,
        };

        await onSubmit(request);
      }
    } catch (err) {
      console.error("Failed to save prosumer:", err);

      const error = err as {
        response?: {
          data?: {
            message?: string;
            error?: string;
            title?: string;
          };
        };
      };

      setError(
        error.response?.data?.message ||
          error.response?.data?.error ||
          error.response?.data?.title ||
          "Unable to save the prosumer.",
      );
    } finally {
      setSubmitting(false);
    }
  };

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
        <div className="modal-header">
          <div>
            <h2>{isEditing ? "Edit Prosumer" : "Register Prosumer"}</h2>

            <p>
              {isEditing
                ? "Update prosumer account information."
                : "Create a new prosumer account."}
            </p>
          </div>

          <button
            type="button"
            className="modal-close-button"
            onClick={onClose}
            disabled={submitting}
          >
            ×
          </button>
        </div>

        {error && <div className="alert alert-error modal-alert">{error}</div>}

        <form className="user-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="prosumer-form-nic">NIC</label>

            <input
              id="prosumer-form-nic"
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

          <div className="form-group">
            <label htmlFor="prosumer-form-name">Full Name</label>

            <input
              id="prosumer-form-name"
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
              placeholder="Enter full name"
              disabled={submitting}
            />
          </div>

          <div className="form-group">
            <label htmlFor="prosumer-form-email">Email</label>

            <input
              id="prosumer-form-email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="Enter email address"
              disabled={submitting}
            />
          </div>

          <div className="form-group">
            <label htmlFor="prosumer-form-phone">Phone</label>

            <input
              id="prosumer-form-phone"
              type="tel"
              value={phone}
              onChange={(event) => setPhone(event.target.value)}
              placeholder="Enter phone number"
              disabled={submitting}
            />
          </div>

          <div className="form-group">
            <label htmlFor="prosumer-form-address">Address</label>

            <textarea
              id="prosumer-form-address"
              value={address}
              onChange={(event) => setAddress(event.target.value)}
              placeholder="Enter address"
              rows={3}
              disabled={submitting}
            />
          </div>

          {!isEditing && (
            <div className="form-group">
              <label htmlFor="prosumer-form-password">Password</label>

              <input
                id="prosumer-form-password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Enter password"
                disabled={submitting}
              />
            </div>
          )}

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
                  : "Register Prosumer"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default ProsumerFormModal;
