package com.portfolio.app.post;

import com.portfolio.app.post.dto.CreatePostRequest;
import com.portfolio.app.post.dto.PostResponse;
import com.portfolio.app.post.dto.UpdatePostRequest;
import com.portfolio.app.user.User;
import com.portfolio.app.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private PostRepository postRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PostService postService;

    // --- helpers ---

    private User makeUser(long id) {
        User user = new User("user@example.com", "hash");
        setField(user, User.class, "id", id);
        return user;
    }

    private Post makePost(long id, User author) {
        Post post = new Post("title", "content", author);
        setField(post, Post.class, "id", id);
        return post;
    }

    private static void setField(Object target, Class<?> clazz, String fieldName, Object value) {
        try {
            var field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- createPost ---

    @Test
    @DisplayName("게시글 작성 성공")
    void createPost_success() {
        User author = makeUser(1L);
        Post savedPost = makePost(10L, author);

        given(userRepository.findById(1L)).willReturn(Optional.of(author));
        given(postRepository.save(any(Post.class))).willReturn(savedPost);

        PostResponse response = postService.createPost(1L, new CreatePostRequest("title", "content"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("title");
        assertThat(response.authorId()).isEqualTo(1L);
        assertThat(response.authorUsername()).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("존재하지 않는 유저로 게시글 작성 → 예외")
    void createPost_userNotFound_throwsException() {
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.createPost(99L, new CreatePostRequest("t", "c")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    // --- listPosts ---

    @Test
    @DisplayName("게시글 목록 조회")
    void listPosts_returnsList() {
        User author = makeUser(1L);
        given(postRepository.findAll()).willReturn(List.of(makePost(1L, author), makePost(2L, author)));

        List<PostResponse> result = postService.listPosts();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("게시글 없을 때 빈 목록 반환")
    void listPosts_empty_returnsEmptyList() {
        given(postRepository.findAll()).willReturn(List.of());

        assertThat(postService.listPosts()).isEmpty();
    }

    // --- getPost ---

    @Test
    @DisplayName("게시글 단건 조회 성공")
    void getPost_success() {
        User author = makeUser(1L);
        given(postRepository.findById(5L)).willReturn(Optional.of(makePost(5L, author)));

        PostResponse result = postService.getPost(5L);

        assertThat(result.id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("존재하지 않는 게시글 조회 → 예외")
    void getPost_notFound_throwsException() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.getPost(99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    // --- updatePost ---

    @Test
    @DisplayName("작성자가 게시글 수정 성공")
    void updatePost_byAuthor_success() {
        User author = makeUser(1L);
        Post post = makePost(5L, author);
        given(postRepository.findById(5L)).willReturn(Optional.of(post));

        PostResponse result = postService.updatePost(1L, false, 5L, new UpdatePostRequest("new title", "new content"));

        assertThat(result.title()).isEqualTo("new title");
        assertThat(result.content()).isEqualTo("new content");
    }

    @Test
    @DisplayName("관리자가 타인 게시글 수정 성공")
    void updatePost_byAdmin_success() {
        User author = makeUser(1L);
        Post post = makePost(5L, author);
        given(postRepository.findById(5L)).willReturn(Optional.of(post));

        PostResponse result = postService.updatePost(999L, true, 5L, new UpdatePostRequest("admin edit", "admin body"));

        assertThat(result.title()).isEqualTo("admin edit");
    }

    @Test
    @DisplayName("비작성자/비관리자가 게시글 수정 → 예외")
    void updatePost_notAuthorized_throwsException() {
        User author = makeUser(1L);
        Post post = makePost(5L, author);
        given(postRepository.findById(5L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.updatePost(2L, false, 5L, new UpdatePostRequest("t", "c")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 수정 → 예외")
    void updatePost_postNotFound_throwsException() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.updatePost(1L, false, 99L, new UpdatePostRequest("t", "c")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }

    // --- deletePost ---

    @Test
    @DisplayName("작성자가 게시글 삭제 성공")
    void deletePost_byAuthor_success() {
        User author = makeUser(1L);
        Post post = makePost(5L, author);
        given(postRepository.findById(5L)).willReturn(Optional.of(post));

        postService.deletePost(1L, false, 5L);

        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("관리자가 타인 게시글 삭제 성공")
    void deletePost_byAdmin_success() {
        User author = makeUser(1L);
        Post post = makePost(5L, author);
        given(postRepository.findById(5L)).willReturn(Optional.of(post));

        postService.deletePost(999L, true, 5L);

        verify(postRepository).delete(post);
    }

    @Test
    @DisplayName("비작성자/비관리자가 게시글 삭제 → 예외")
    void deletePost_notAuthorized_throwsException() {
        User author = makeUser(1L);
        Post post = makePost(5L, author);
        given(postRepository.findById(5L)).willReturn(Optional.of(post));

        assertThatThrownBy(() -> postService.deletePost(2L, false, 5L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    @DisplayName("존재하지 않는 게시글 삭제 → 예외")
    void deletePost_postNotFound_throwsException() {
        given(postRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> postService.deletePost(1L, false, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Post not found");
    }
}
