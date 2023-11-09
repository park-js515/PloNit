export interface CrewInterface {
  id?: number;
  name: string;
  cntPeople?: number;
  crewImage: string;
  region: string;
  introduce?: string;
  rankinginfo?: string;
  totalRanking?: number;
  avgRanking?: number;
  crewMasterProfileImage?: string;
  crewMasterNickname?: string;
  notice?: string;
  isCrewMaster?: boolean;
  isMyCrew?: boolean;
}

export interface FeedInterface {
  crewId?: number;
  id: number;
  nickname?: string;
  profileImage?: string;
  content?: string;
  feedPictures?: any;
  likeCount?: number;
  islike?: boolean;
  isMine?: boolean;
  comments?: any;
}

export interface CrewpingInterface {
  id?: number;
  name: string;
  crewpingImage?: string;
  masterMemberId?: number;
  masterNickname?: string;
  masterImage?: string;
  place: string;
  startDate?: string;
  endDate?: string;
  cntPeople?: number;
  maxPeople?: number | string;
  introduce?: string;
}

export interface CommentInterface {
  feedId?: number;
  commentId?: number;
  nickname?: string;
  profileImage?: string;
  content?: string;
}

export interface CrewAllowInterface {
  crewId?: number;
  commentId?: number;
  status?: boolean;
}

export interface MemberInterface {
  crewMemberId: number;
  profileImage: string;
  nickname: string;
}

export interface NoticeInterface {
  crewId: number;
  content: string;
}

export interface MemberListProps {
  title: string;
  location?: string;
  memberCount?: string;
  imageUrl: string; // 이미지 URL 추가
  showApproveButton?: boolean; // 승인 버튼 표시 여부를 위한 prop
  onApprove?: () => void; // 승인 버튼 클릭 시 호출할 함수
}
