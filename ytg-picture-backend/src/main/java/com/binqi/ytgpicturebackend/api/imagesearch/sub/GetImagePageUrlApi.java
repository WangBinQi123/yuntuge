package com.binqi.ytgpicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONUtil;
import com.binqi.ytgpicturebackend.exception.BusinessException;
import com.binqi.ytgpicturebackend.exception.ErrorCode;
import com.luciad.imageio.webp.WebPImageReaderSpi;
import com.luciad.imageio.webp.WebPReadParam;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {

    static {
        ImageIO.scanForPlugins();
        new WebPImageReaderSpi();
    }

    /**
     * 获取百度图像搜索结果页链接
     */
    public static String getImagePageUrl(String imageUrl) throws IOException {
// 构造表单上传请求
        Map formData = new HashMap<>();
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        File originalFile = null;
        File uploadFile = null;

// 判断是否是 webp 格式并转换
        if (imageUrl.toLowerCase().endsWith(".webp")) {
            originalFile = downloadImageToTempFile(imageUrl);
            uploadFile = File.createTempFile("converted_", ".png");
            convertWebpToPng(originalFile, uploadFile);
            formData.put("image", uploadFile);
        }
        else{
            formData.put("image", imageUrl);
        }

        long uptime = System.currentTimeMillis();
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        try {
            HttpResponse response = HttpRequest.post(url)
                    .header("acs-token", RandomUtil.randomString(1))
                    .form(formData)
                    .timeout(5000)
                    .execute();

            if (HttpStatus.HTTP_OK != response.getStatus()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            Map result = JSONUtil.toBean(response.body(), Map.class);
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }

            Map data = (Map) result.get("data");
            String rawUrl = (String) data.get("url");
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);

            if (searchResultUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }

            return searchResultUrl;
        } catch (Exception e) {
            log.error("图片搜索失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        } finally {
// 删除临时文件
            if (originalFile != null && originalFile.exists()) originalFile.delete();
            if (uploadFile != null && uploadFile.exists()) uploadFile.delete();
        }
    }

    /**
     * 下载图片到临时文件
     */
    private static File downloadImageToTempFile(String imageUrl) throws IOException {
        String extension = getFileExtension(imageUrl);
        File tempFile = File.createTempFile("upload_", "." + extension);
        try (InputStream in = new URL(imageUrl).openStream(); FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
        return tempFile;
    }

    /**
     * 从 URL 获取文件扩展名（默认返回 png）
     */
    private static String getFileExtension(String url) {
        try {
            String path = new URL(url).getPath();
            int index = path.lastIndexOf('.');
            if (index != -1) {
                return path.substring(index + 1).toLowerCase();
            }
        } catch (Exception ignored) {
        }
        return "png";
    }

    /**
     * WebP 转 PNG
     */
    private static void convertWebpToPng(File webpFile, File pngFile) throws IOException {
        ImageReader reader = ImageIO.getImageReadersByMIMEType("image/webp").next();
        WebPReadParam readParam = new WebPReadParam();
        readParam.setBypassFiltering(true);
        reader.setInput(new FileImageInputStream(webpFile));
        BufferedImage image = reader.read(0, readParam);
        ImageIO.write(image, "png", pngFile);
    }

    public static void main(String[] args) throws IOException {
        String imageUrl = "https://qingyun-1318456424.cos.ap-shanghai.myqcloud.com/space/1916859323002720257/2025-04-28_$sSiHWrvDoH2ZLergy.webp";
        String result = getImagePageUrl(imageUrl);
        System.out.println(result);
    }
}