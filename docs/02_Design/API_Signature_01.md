| **자원** | **기능** | **Method** | **URI**                          |
| --- | --- | --- |----------------------------------|
| 회원 | 회원가입 | POST | /api/v1/auth/signup              |
| 회원 | 로그인 | POST | /api/v1/auth/signin              |
| 회원 | 로그아웃 | POST | /api/v1/auth/logout              |
| 회원 | 본인 정보 조회 | GET | /api/v1/users/me                 |
| 회원 | 본인 프로필 수정 | PATCH | /api/v1/users/me                 |
| 회원 | 회원탈퇴 | DELETE | /api/v1/users/me                 |
| 회원 | 본인 게시물 조회 | GET | /api/v1/users/me/posts           |
| 회원 | 본인 활동 내역 조회 | GET | /api/v1/users/me/activities      |
| 회원 | 본인 그룹 조회 | GET | /api/v1/users/me/groups          |
| 회원 | 다른 사용자 프로필 조회 | GET | /api/v1/users/:userId            |
| 회원 | 팔로우 | POST | /api/v1/users/:userId/follow     |
| 회원 | 언팔로우 | DELETE | /api/v1/users/:userId/follow     |
| 게시물 | 게시글 목록 조회 | GET | /api/v1/posts                    |
| 게시물 | 게시글 상세 조회 | GET | /api/v1/posts/:postId            |
| 게시물 | 게시글 작성 | POST | /api/v1/posts                    |
| 게시물 | 게시글 수정 | PATCH | /api/v1/posts/:postId            |
| 게시물 | 게시글 삭제 | DELETE | /api/v1/posts/:postId            |
| 게시물 | 게시글 좋아요 | POST | /api/v1/posts/:postId/like       |
| 게시물 | 게시글 좋아요 취소 | DELETE | /api/v1/posts/:postId/like       |
| 댓글 | 댓글 조회 | GET | /api/v1/posts/:postId/comments   |
| 댓글 | 댓글 작성(답글 포함) | POST | /api/v1/posts/:postId/comments   |
| 댓글 | 댓글 수정 | PATCH | /api/v1/comments/:commentId      |
| 댓글 | 댓글 삭제 | DELETE | /api/v1/comments/:commentId      |
| 댓글 | 댓글 좋아요 | POST | /api/v1/comments/:commentId/like |
| 댓글 | 댓글 좋아요 취소 | DELETE | /api/v1/comments/:commentId/like |
| 그룹 | 그룹 목록 조회 | GET | /api/v1/groups                   |
| 그룹 | 그룹 상세 조회 | GET | /api/v1/groups/:groupId          |
| 그룹 | 그룹 생성 | POST | /api/v1/groups                   |
| 그룹 | 그룹 삭제 | DELETE | /api/v1/groups/:groupId          |
| 그룹 | 그룹 참여 | POST | /api/v1/groups/:groupId/join     |
| 그룹 | 그룹 나가기 | DELETE | /api/v1/groups/:groupId/leave    |
| 그룹 | 그룹원 조회 | GET | /api/v1/groups/:groupId/members  |
| 그룹 | 그룹 내 랭킹 조회 | GET | /api/v1/groups/:groupId/rank     |