package com.example.graduation.controller;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.graduation.common.ApiResponse;
import com.example.graduation.dto.UserBackgroundResponse;
import com.example.graduation.entity.User;
import com.example.graduation.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserPreferenceController {

    private final UserMapper userMapper;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5MB
    private static final Set<String> ALLOWED_EXT = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp", "gif"));

    private static final int DEFAULT_SCALE = 100;
    private static final int DEFAULT_POS = 50;
    private static final double DEFAULT_OVERLAY_ALPHA = 0.78;
    private static final double DEFAULT_CONTENT_ALPHA = 1.00;
    private static final int DEFAULT_CONTENT_BLUR = 0;

    public UserPreferenceController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    private Long getCurrentUserId(jakarta.servlet.http.HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    private UserBackgroundResponse toResponse(User user) {
        if (user == null) {
            return new UserBackgroundResponse(null, DEFAULT_SCALE, DEFAULT_POS, DEFAULT_POS, DEFAULT_OVERLAY_ALPHA, DEFAULT_CONTENT_ALPHA, DEFAULT_CONTENT_BLUR);
        }
        return new UserBackgroundResponse(
                user.getBackgroundUrl(),
                user.getBackgroundScale() == null ? DEFAULT_SCALE : user.getBackgroundScale(),
                user.getBackgroundPosX() == null ? DEFAULT_POS : user.getBackgroundPosX(),
                user.getBackgroundPosY() == null ? DEFAULT_POS : user.getBackgroundPosY(),
                user.getBgOverlayAlpha() == null ? DEFAULT_OVERLAY_ALPHA : user.getBgOverlayAlpha(),
                user.getContentAlpha() == null ? DEFAULT_CONTENT_ALPHA : user.getContentAlpha(),
                user.getContentBlur() == null ? DEFAULT_CONTENT_BLUR : user.getContentBlur()
        );
    }

    private static int clampInt(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double clampDouble(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    @GetMapping("/background")
    public ApiResponse<UserBackgroundResponse> getBackground(jakarta.servlet.http.HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        User user = userMapper.selectById(userId);
        return ApiResponse.success(toResponse(user));
    }

    @PostMapping("/background")
    public ApiResponse<UserBackgroundResponse> uploadBackground(
            @RequestParam("file") MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request
    ) throws IOException {
        Long userId = getCurrentUserId(request);
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new RuntimeException("图片过大，请上传 5MB 以内的图片");
        }

        String original = file.getOriginalFilename();
        String ext = StringUtils.getFilenameExtension(original);
        ext = ext == null ? "" : ext.toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new RuntimeException("仅支持 jpg/jpeg/png/webp/gif 格式");
        }

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path dir = root.resolve("backgrounds");
        Files.createDirectories(dir);

        String filename = "u" + userId + "-" + UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(filename).normalize();
        if (!target.startsWith(dir)) {
            throw new RuntimeException("非法文件路径");
        }
        file.transferTo(target.toFile());

        String url = "/uploads/backgrounds/" + filename;

        // 更新用户背景 URL（保留其它字段）
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setBackgroundUrl(url);
        // 若旧数据缺省，顺手补上默认外观参数（避免前端拿到 null）
        if (user.getBackgroundScale() == null) user.setBackgroundScale(DEFAULT_SCALE);
        if (user.getBackgroundPosX() == null) user.setBackgroundPosX(DEFAULT_POS);
        if (user.getBackgroundPosY() == null) user.setBackgroundPosY(DEFAULT_POS);
        if (user.getBgOverlayAlpha() == null) user.setBgOverlayAlpha(DEFAULT_OVERLAY_ALPHA);
        if (user.getContentAlpha() == null) user.setContentAlpha(DEFAULT_CONTENT_ALPHA);
        if (user.getContentBlur() == null) user.setContentBlur(DEFAULT_CONTENT_BLUR);
        userMapper.updateById(user);

        return ApiResponse.success(toResponse(user));
    }

    /**
     * 更新外观参数（缩放/位置/透明度等），不修改 backgroundUrl。
     */
    @PutMapping("/background")
    public ApiResponse<UserBackgroundResponse> updateAppearance(
            @RequestBody UserBackgroundResponse body,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        Long userId = getCurrentUserId(request);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        Integer scale = body.getBackgroundScale();
        Integer posX = body.getBackgroundPosX();
        Integer posY = body.getBackgroundPosY();
        Double overlay = body.getBgOverlayAlpha();
        Double contentAlpha = body.getContentAlpha();
        Integer blur = body.getContentBlur();

        user.setBackgroundScale(scale == null ? user.getBackgroundScale() : clampInt(scale, 50, 200));
        user.setBackgroundPosX(posX == null ? user.getBackgroundPosX() : clampInt(posX, 0, 100));
        user.setBackgroundPosY(posY == null ? user.getBackgroundPosY() : clampInt(posY, 0, 100));
        user.setBgOverlayAlpha(overlay == null ? user.getBgOverlayAlpha() : clampDouble(overlay, 0.00, 1.00));
        user.setContentAlpha(contentAlpha == null ? user.getContentAlpha() : clampDouble(contentAlpha, 0.40, 1.00));
        user.setContentBlur(blur == null ? user.getContentBlur() : clampInt(blur, 0, 24));

        userMapper.updateById(user);
        User updated = userMapper.selectById(userId);
        return ApiResponse.success(toResponse(updated));
    }

    @DeleteMapping("/background")
    public ApiResponse<UserBackgroundResponse> resetBackground(jakarta.servlet.http.HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        // 注意：部分 MyBatis-Plus 配置会跳过 null 字段更新。
        // 这里用 UpdateWrapper 显式置空 + 重置外观参数为默认值，确保持久化生效。
        int updated = userMapper.update(
                null,
                new UpdateWrapper<User>()
                        .eq("id", userId)
                        .set("background_url", null)
                        .set("background_scale", DEFAULT_SCALE)
                        .set("background_pos_x", DEFAULT_POS)
                        .set("background_pos_y", DEFAULT_POS)
                        .set("bg_overlay_alpha", DEFAULT_OVERLAY_ALPHA)
                        .set("content_alpha", DEFAULT_CONTENT_ALPHA)
                        .set("content_blur", DEFAULT_CONTENT_BLUR)
        );
        if (updated <= 0) {
            throw new RuntimeException("用户不存在");
        }
        return ApiResponse.success(new UserBackgroundResponse(null, DEFAULT_SCALE, DEFAULT_POS, DEFAULT_POS, DEFAULT_OVERLAY_ALPHA, DEFAULT_CONTENT_ALPHA, DEFAULT_CONTENT_BLUR));
    }
}

