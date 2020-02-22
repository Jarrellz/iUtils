package com.demo.aop.demo;

import com.sun.tools.javac.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * 从ftp服务器下载文件
 *
 * 依赖
 * <dependency>
 *     <groupId>commons-net</groupId>
 *     <artifactId>commons-net</artifactId>
 *     <version>3.6</version>
 * </dependency>
 *
 * @author Jarrellz 2020/02/22
 */
public class FtpDownloadFileUtil {
    /**
     * 本地字符编码
     */
    private static final String LOCAL_CHARSET = "GBK";

    /**
     * FTP协议里面，规定文件名编码为iso-8859-1
     */
    private static final String SERVER_CHARSET = "ISO-8859-1";

    private  static  org.slf4j.Logger log = LoggerFactory.getLogger(FtpDownloadFileUtil.class);

    public static void main(String[] args) {
        String ip = "";
        int port = 0;
        String userName = "";
        String password = "";
        String savePath = "";

        List<String> ftpPaths = List.of(
            "/zsl/pc/95/d/index.html");
        downloadFile(ip,port,userName,password,ftpPaths,savePath,false);
    }


    /**
     *
     * @param host ip
     * @param port 端口，无端口传0
     * @param username
     * @param password
     * @param paths 路径集合
     * @param savePath 保存到本地到路径
     * @param useFtpDirectory 是否使用ftp服务器目录结构保存到本地
     *
     */
    public  static void downloadFile(String host,int port,String username,String password,
                                     List<String> paths,String savePath,boolean useFtpDirectory){
        FTPClient ftp = new FTPClient();
        try {
            int reply;
            ftp.connect(host,port == 0 ? 21 : port);
            // 如果采用默认端口，可以使用ftp.connect(host)的方式直接连接FTP服务器
            // 登录
            ftp.login(username, password);
            reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                log.error("=======ftp服务器登录失败=======");
                ftp.disconnect();
            }
            for(String ftpPath : paths){
                String fileName = ftpPath.substring(ftpPath.lastIndexOf('/') + 1);
                String path = ftpPath.substring(0,ftpPath.lastIndexOf('/'));
                log.debug("准备下载:{}" , ftpPath);
                // 转移到FTP服务器目录
                ftp.changeWorkingDirectory(path);
                FTPFile[] fs = ftp.listFiles();
                boolean flag = false;
                for (FTPFile ff : fs) {
                    String name = new String(ff.getName().getBytes(SERVER_CHARSET),LOCAL_CHARSET);
                    if (name.equals(fileName)) {
                        byte[] bytes = null;
                        try(ByteArrayOutputStream os = new ByteArrayOutputStream()){
                            //写入输出流
                            ftp.retrieveFile(new String(name.getBytes(LOCAL_CHARSET),SERVER_CHARSET),os);
                            bytes = os.toByteArray();
                            os.flush();
                        }
                        log.debug("文件大小：{}",String.format("%.2f", bytes.length / 1024d) + "KB");
                        if(bytes.length > 0){
                            flag = true;
                        }
                        String saveFilePath = useFtpDirectory ? savePath + ftpPath : savePath + fileName;
                        File file = new File(saveFilePath);
                        log.debug("保存目录：{}",saveFilePath);
                        // 校验文件夹目录是否存在，不存在就创建一个目录
                        if (!file.getParentFile().exists()) {
                            file.getParentFile().mkdirs();
                        }
                        try (OutputStream out = new FileOutputStream(file)) {
                            out.write(bytes, 0, bytes.length);
                            out.flush();
                        }
                    }
                }
                if(!flag){
                    log.error("下载失败：{}" , ftpPath);
                }
            }


        } catch (IOException ex) {
            log.error("=======ftp文下载IO异常=======:{}" , ex.getMessage());
        }finally {
            try {
                ftp.logout();
            } catch (IOException e) {
                log.error("=======断开ftp异常=======:{}" , e.getMessage());
            }
        }
    }
}
