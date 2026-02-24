package com.portfolio.app.post;

import com.portfolio.app.post.dto.CreatePostRequest;
import com.portfolio.app.post.dto.PostResponse;
import com.portfolio.app.post.dto.UpdatePostRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('WRITER', 'ADMIN')")
    public PostResponse createPost(@AuthenticationPrincipal String userId,
                                   @Valid @RequestBody CreatePostRequest request) {
        return postService.createPost(Long.parseLong(userId), request);
    }

    @GetMapping
    public List<PostResponse> listPosts() {
        return postService.listPosts();
    }

    @GetMapping("/{id}")
    public PostResponse getPost(@PathVariable Long id) {
        return postService.getPost(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('WRITER', 'ADMIN')")
    public PostResponse updatePost(@AuthenticationPrincipal String userId,
                                   Authentication authentication,
                                   @PathVariable Long id,
                                   @Valid @RequestBody UpdatePostRequest request) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return postService.updatePost(Long.parseLong(userId), isAdmin, id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('WRITER', 'ADMIN')")
    public void deletePost(@AuthenticationPrincipal String userId,
                           Authentication authentication,
                           @PathVariable Long id) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        postService.deletePost(Long.parseLong(userId), isAdmin, id);
    }
}
