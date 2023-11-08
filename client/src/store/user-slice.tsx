import { createSlice } from "@reduxjs/toolkit";
import { act } from "react-dom/test-utils";

const initialState = {
  isLogin: false,
  accessToken: "",
  refreshToken: "",
  profileImg: "",
  email: "",
  nickname: "",
  name: "",
  gender: 0,
  birthday: "",
  dongCode: 0,
  height: 0,
  weight: 0,
  id_1365: "",
};

const userSlice = createSlice({
  name: "user",
  initialState,
  reducers: {
    loginHandler: (state, action) => {
      state.isLogin = true;
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.profileImg = action.payload.profileImg;
      state.nickname = action.payload.nickname;
    },
    logout: (state) => {
      state.isLogin = false;
    },
    addInfoHandler: (state, action) => {
      state.name = action.payload.name;
      state.nickname = action.payload.nickname;
      state.gender = action.payload.gender;
      state.birthday = action.payload.birth;
      state.dongCode = action.payload.region;
    },
  },
});

// export const { loginHandler, logout } = userSlice.actions;
export const userActions = userSlice.actions;
export default userSlice.reducer;
