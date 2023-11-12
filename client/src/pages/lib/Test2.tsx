import React, { useRef } from "react";
import useSocket from "components/plogging/functions/useSocket";
import { Client } from "@stomp/stompjs";

import { useDispatch } from "react-redux";
import * as Crewping from "store/crewping-slice";

const Test2 = () => {
  const dispatch = useDispatch();
  const stompClient = useRef<Client | null>(null);
  useSocket({ stompClient: stompClient, roomId: "test", senderId: "박주성" });

  return (
    <div>
      <button
        onClick={() => {
          dispatch(Crewping.setGetLocation(true));
        }}
      >
        위치 보내기
      </button>
    </div>
  );
};

export default Test2;
