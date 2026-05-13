package com.nz.admin.framework.protection.support;

/**
 * 谨慎的输入清理工具。
 */
public class XssCleaner {

    public String clean(String input, boolean allowRichText) {
        if (input == null || input.isBlank() || allowRichText) {
            return input;
        }
        return input.replace("<script", "&lt;script")
                .replace("</script>", "&lt;/script&gt;")
                .replace("javascript:", "")
                .replace("onerror=", "")
                .replace("onload=", "");
    }
}
