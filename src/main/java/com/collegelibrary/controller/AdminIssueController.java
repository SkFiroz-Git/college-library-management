package com.collegelibrary.controller;

import com.collegelibrary.dto.IssueCreateRequest;
import com.collegelibrary.dto.IssueResponse;
import com.collegelibrary.dto.PageResponse;
import com.collegelibrary.enums.IssueStatus;
import com.collegelibrary.service.IssueService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Admin + librarian: lend and collect books. */
@RestController
@RequestMapping("/api/admin/issues")
public class AdminIssueController {

    private final IssueService issueService;

    public AdminIssueController(IssueService issueService) {
        this.issueService = issueService;
    }

    @GetMapping
    public PageResponse<IssueResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) IssueStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return issueService.list(search, status, page, size);
    }

    @GetMapping("/overdue")
    public PageResponse<IssueResponse> overdue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return issueService.overdue(page, size);
    }

    @PostMapping
    public ResponseEntity<IssueResponse> issue(@Valid @RequestBody IssueCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(issueService.issue(request));
    }

    @PostMapping("/{id}/return")
    public IssueResponse returnBook(@PathVariable Long id) {
        return issueService.returnBook(id);
    }
}
