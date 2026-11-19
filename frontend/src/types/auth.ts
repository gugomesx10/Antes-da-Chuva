import type { Role } from './incident';
export interface User { id: number; name: string; email: string; role: Role; neighborhood: string | null }
export interface AuthResponse { accessToken: string; tokenType: string; expiresIn: number; user: User }
export interface AuthSession extends AuthResponse { expiresAt: number }
export interface RegisterInput { name: string; email: string; password: string; neighborhood: string }
