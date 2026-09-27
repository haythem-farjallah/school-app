import { createSlice, PayloadAction } from "@reduxjs/toolkit";
import { token } from "@/lib/token";

interface User {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  role: string;
  permissions?: string[];
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
}

const initialState: AuthState = {
  user: token.user || null,
  accessToken: token.access || null,
  refreshToken: token.refresh || null,
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    loginSuccess(
      state,
      action: PayloadAction<{
        user: User;
        accessToken: string;
        refreshToken: string;
      }>
    ) {
      const { user, accessToken, refreshToken } = action.payload;
      state.user = user;
      state.accessToken = accessToken;
      state.refreshToken = refreshToken;
      
      // Persist all data to localStorage
      token.access = accessToken;
      token.refresh = refreshToken;
      token.user = user;
    },
    // A refresh replaces only the access token; the refresh token and user stay the same.
    accessTokenRefreshed(state, action: PayloadAction<string>) {
      state.accessToken = action.payload;
    },
    // Add action to completely reset auth state
    resetAuth: () => ({
      user: null,
      accessToken: null,
      refreshToken: null,
    }),
  },
});

export const { loginSuccess, accessTokenRefreshed, resetAuth } = authSlice.actions;
export default authSlice.reducer;
