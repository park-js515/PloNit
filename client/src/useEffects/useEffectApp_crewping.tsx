import React, { useEffect, useRef } from "react";
import getGPS from "components/plogging/functions/getGPS";
import { ploggingType } from "types/ploggingTypes";
import useSocket from "components/plogging/functions/useSocket";
import { Client } from "@stomp/stompjs";
import Swal from "sweetalert2";

import { useNavigate, useLocation } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { rootState } from "store/store";
import * as P from "store/plogging-slice";

import { startPlogging } from "api/lib/plogging";

function useEffectApp_Crewping() {
  const navigate = useNavigate();
  const location = useLocation();
  const dispatch = useDispatch();
  const worker = new Worker(new URL(`workers/worker.js`, import.meta.url));
  const stompClient = useRef<Client | null>(null);
  const nowType = useSelector<rootState, ploggingType>((state) => {
    return state.plogging.ploggingType;
  });
  const roomId = useSelector<rootState, string>((state) => {
    return state.crewping.roomId;
  });
  const senderId = useSelector<rootState, string>((state) => {
    return state.crewping.senderId;
  });
  const isLoading = useSelector<rootState, boolean>((state) => {
    return state.crewping.isLoading;
  });
  const crewpingStart = useSelector<rootState, boolean>((state) => {
    return state.crewping.crewpingStart;
  });
  const crewpingEnd = useSelector<rootState, boolean>((state) => {
    return state.crewping.crewpingEnd;
  });
  const accessToken = useSelector<rootState, string>((state) => {
    return state.user.auth.accessToken;
  });
  const weight = useSelector<rootState, number>((state) => {
    return state.user.info.weight;
  });

  useEffect(() => {
    if (isLoading) {
      useSocket({ stompClient, roomId, senderId });
    }
  }, [isLoading]);

  useEffect(() => {
    if (crewpingStart) {
      if (weight > 0) {
        dispatch(P.setKg(weight));
      }
      getGPS().then((response) => {
        const { latitude, longitude } = response.coords;
        startPlogging({
          accessToken: accessToken,
          type: "CREWPING",
          latitude: latitude,
          longitude: longitude,
          success: (response) => {
            console.log("크루핑 요청!");
            console.log(response);
            dispatch(P.setCrewpingId(response.data.resultBody));

            if (location.pathname !== "/plogging") {
              navigate("/plogging");
            }
          },
          fail: (error) => {
            console.error(error);
          },
        });
      });
    }
  }, [crewpingStart]);

  useEffect(() => {
    if (crewpingEnd) {
      dispatch(P.setIsEnd(true));
      navigate("/plogging/complete");
      stompClient.current?.deactivate();
    }
  }, [crewpingEnd]);
}

export default useEffectApp_Crewping;