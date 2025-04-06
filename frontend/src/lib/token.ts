const ACCESS  = "accessToken";
const REFRESH = "refreshToken";

export const token = {
  get access() {
    return localStorage.getItem(ACCESS);
  },
  set access(v: string | null) {
    if (v) {
      localStorage.setItem(ACCESS, v);
    } else {
      localStorage.removeItem(ACCESS);
    }
  },

  get refresh() {
    return localStorage.getItem(REFRESH);
  },
  set refresh(v: string | null) {
    if (v) {
      localStorage.setItem(REFRESH, v);
    } else {
      localStorage.removeItem(REFRESH);
    }
  },

  clear() {
    localStorage.removeItem(ACCESS);
    localStorage.removeItem(REFRESH);
  },
};
