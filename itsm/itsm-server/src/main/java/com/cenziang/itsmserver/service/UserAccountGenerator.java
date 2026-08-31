package com.cenziang.itsmserver.service;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.HanyuPinyinVCharType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 用户账号生成器。
 * <p>
 * 生成 6 位数字自增 user_id（从管理员 000001 开始），以及
 * 基于真实姓名拼音的邮箱（真实姓名拼音@cza.com，重名自动加数字后缀）。
 * </p>
 */
@Component
public class UserAccountGenerator {
    private final JdbcTemplate jdbcTemplate;

    public UserAccountGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 生成下一个 6 位数字用户主键。
     */
    public String nextUserId(String tenantId) {
        Long max = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(CAST(user_id AS UNSIGNED)), 0) FROM app_user WHERE tenant_id = ?",
                Long.class,
                tenantId
        );
        long next = (max == null ? 0L : max) + 1;
        return String.format("%06d", next);
    }

    /**
     * 生成唯一邮箱：真实姓名拼音@cza.com，重名时追加数字后缀。
     */
    public String generateEmail(String tenantId, String displayName) {
        String base = toPinyin(displayName);
        String email = base + "@cza.com";
        int suffix = 0;
        while (emailExists(tenantId, email)) {
            suffix++;
            email = base + suffix + "@cza.com";
        }
        return email;
    }

    /**
     * 返回邮箱前缀（真实姓名拼音），不做去重。
     */
    public String emailPrefix(String displayName) {
        return toPinyin(displayName);
    }

    private boolean emailExists(String tenantId, String email) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM app_user WHERE tenant_id = ? AND contact_email = ?",
                Integer.class,
                tenantId,
                email
        );
        return count != null && count > 0;
    }

    /**
     * 将姓名转为小写拼音（中文转拼音，字母数字原样保留，其它字符忽略）。
     */
    private String toPinyin(String text) {
        if (text == null || text.isBlank()) {
            return "user";
        }
        HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
        format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
        format.setVCharType(HanyuPinyinVCharType.WITH_V);

        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            String s = String.valueOf(c);
            if (s.matches("[\\u4E00-\\u9FA5]")) {
                try {
                    String[] arr = PinyinHelper.toHanyuPinyinStringArray(c, format);
                    if (arr != null && arr.length > 0) {
                        sb.append(arr[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                    // 忽略无法转换的字符
                }
            } else if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.length() == 0 ? "user" : sb.toString();
    }
}
