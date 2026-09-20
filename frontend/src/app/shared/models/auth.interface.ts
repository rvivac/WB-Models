export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token?: string;
  accessToken?: string;
  tokenType?: string;
  name?: string;
  adminName?: string;
  email?: string;
  adminEmail?: string;
  role?: string;
  expiresIn?: number;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface MessageResponse {
  message: string;
}

export interface AdminUser {
  name: string;
  email: string;
  role: string;
}
