export interface User {
  id: string;
  email: string;
  fullName: string;
  phoneNumber: string | null;
  role: string;
  passkeyEnabled: boolean;
  createdAt: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  message: string;
  details: string[];
}
