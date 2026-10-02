export interface LoginRequest {
  email: string;
  password: string;
}

export interface AdminUser {
  id?: string;
  name: string;
  email: string;
  role: 'ADMIN' | 'SCOUT' | 'SUPER_ADMIN' | 'WEBMASTER' | string;
  isActive?: boolean;
  is2faEnabled?: boolean;
  mustChangePassword?: boolean;
  lastLoginAt?: string;
}

export interface AuthResponse {
  token?: string;
  refreshToken?: string;
  type?: string;
  user?: AdminUser;
  requires2fa?: boolean;
  tempToken?: string;
  // Campos de compatibilidade com backend Spring Boot
  accessToken?: string;
  tokenType?: string;
  expiresIn?: number;
  adminName?: string;
  adminEmail?: string;
  name?: string;
  email?: string;
  role?: string;
  jwt?: string;
  id?: string;
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
  confirmPassword?: string;
}

export interface MessageResponse {
  message: string;
}
