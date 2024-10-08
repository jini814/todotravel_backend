package org.example.todotravel.domain.chat.repository;

import org.example.todotravel.domain.chat.entity.ChatRoom;
import org.example.todotravel.domain.chat.entity.ChatRoomUser;
import org.example.todotravel.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {

    @Query("""
        SELECT cru FROM ChatRoomUser cru
        WHERE cru.chatRoom.roomId = :roomId
        ORDER BY cru.chatRoomUserId ASC 
        LIMIT 1
        """)
    Optional<ChatRoomUser> findFirstUserInChatRoom(@Param("roomId") Long roomId);

    @Query("""
        SELECT cru.chatRoom FROM ChatRoomUser cru
        WHERE cru.user.userId = :userId
        ORDER BY cru.chatRoom.roomDate DESC
        """)
    List<ChatRoom> findChatRoomByUserId(@Param("userId") Long userId);

    Optional<ChatRoomUser> findByUserAndChatRoom(User user, ChatRoom chatRoom);

    List<ChatRoomUser> findByChatRoomRoomId(Long roomId);

    Optional<ChatRoomUser> findByUserAndChatRoomRoomId(User user, Long roomId);

    boolean existsByChatRoomAndUser(ChatRoom chatRoom, User user);

    @Modifying
    @Query("DELETE FROM ChatRoomUser cru WHERE cru.chatRoom.roomId = :roomId")
    void deleteByChatRoom(@Param("roomId") Long roomId);

    @Modifying
    @Query("DELETE FROM ChatRoomUser cru WHERE cru.user = :user")
    void deleteByChatRoomUser(@Param("user") User user);
}
