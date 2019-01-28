package common;

import jcifs.smb.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class NASUtil {


    static Logger logger = Logger.getLogger(NASUtil.class);

    public static void getRemoteFiles(){

        String remoteServer = "192.168.2.10";
        String remoteServerFolder = "\\\\Diskstation\\取込";

        String storageFolder = Constants.ROOT_DIR + "\\kowa\\import\\source\\stock";
        String syncFolder2 = Constants.ROOT_DIR + "\\kowa\\import\\source\\CROSSMALL";

        File file1 = new File(storageFolder);
        if(!file1.exists()){
            logger.info(storageFolder + " is not exists ");
            return;
        }

        File[] storageSubFolder= file1.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (int i = 0; i < storageSubFolder.length; i++) {
            File dir = storageSubFolder[i];
            File[] toTransferFiles = dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isFile();
                }
            });

            for (File toTransferFile : toTransferFiles) {

            }
        }
    }

    public static void main(String[] args) throws Exception{


//        byte[] bytes = "取".getBytes("UTF-8");
//        IntStream.range(0,bytes.length).forEach(item->{
//            System.out.println(bytes[item]);
//        });

//        byte[] a = {-17,-65,-67,-26,-115,-98};
//
//        Set<Map.Entry<String, Charset>> entries = Charset.availableCharsets().entrySet();
//        entries.forEach(item->{
//            Charset value = item.getValue();
//            System.out.println( item.getKey()+ ":" + new String(a, value));
////            System.out.println(item.getKey() + " : " + item.getValue());
//        });



//        byte[] b = {-27,-113,-106,-24,-66,-68};
//        System.out.println(new String(a));
//        System.out.println(new String(b,"UTF-8"));
//        System.out.println("取込".getBytes("utf-8").length);

//        getFtpFiles();
        getNASFileBySmb();
    }


    static int port = 21;
    static String server = "192.168.2.10";
    static String user = "torikomi";
    static String pass = "ylg@%p";

//        static String server = "ftp11.gmoserver.jp";
//    static String user = "sd0464952@gmoserver.jp";
//    static String pass = "zKRs5K64";


    public static void getNASFileBySmb() {
        try {
            NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null,user,pass);


            String pFolderPath = "smb://" + server + "/取込/kowa/import/source/stock/";
//            SmbFile sFile = new SmbFile("smb://"+ server +"/取込/", auth);
            SmbFile sFile = new SmbFile(pFolderPath, auth);

            if(!sFile.exists() || !sFile.isDirectory()){
                throw new Exception(pFolderPath + " not exists ");
            }

            String[] storageFolders = sFile.list(new SmbFilenameFilter() {
                @Override
                public boolean accept(SmbFile dir, String name) throws SmbException {
                    return dir.isDirectory();
                }
            });

            logger.debug(" found subfolders " + storageFolders.length);

            for (int i = 0; i < storageFolders.length; i++) {

                String folder = storageFolders[i];

                SmbFile realFileFolder = new SmbFile(pFolderPath + folder +"/",auth);

                SmbFile[] smbFiles = realFileFolder.listFiles(new SmbFileFilter() {
                    @Override
                    public boolean accept(SmbFile file) throws SmbException {
                        return file.isFile();
                    }
                });

                if (smbFiles.length > 0) {
                    for (int i1 = 0; i1 < smbFiles.length; i1++) {
                        SmbFile needFile = smbFiles[i1];
                        logger.debug("found file :" + needFile.getName());
                        logger.debug("found file fullpath : " + needFile.getCanonicalPath());

                        String localPath = Constants.STOCK_SOURCE_PATH + "/" + folder + "/" + needFile.getName();
                        InputStream inputStream = needFile.getInputStream();
                        IOUtils.copy(inputStream, new FileOutputStream(localPath));
                        inputStream.close();
                        needFile.delete();
                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void getFtpFiles(){
        try {

//            System.out.println("取込".equals("取込"));

            FTPClient ftpClient = new FTPClient();
            ftpClient.setControlEncoding("UTF-8");
//            ftpClient.setAutodetectUTF8(true);
            ftpClient.connect(server, port);
            boolean login = ftpClient.login(user, pass);
            logger.debug("connect to ftp server " + server + " , success is " + login);
            ftpClient.enterLocalPassiveMode();
//            ftpClient.enterLocalActiveMode();
            ftpClient.enterLocalActiveMode();

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

//            String remoteServerFolder = "取込";

//            FTPFile[] ftpFiles = ftpClient.listFiles("issoh.co.jp\\kowa\\import\\source\\stock");
//            String rootPath = "work.issoh.co.jp/Diskstation/取込";
//            String subRootPath = "kowa/import/source/stock";


            ftpClient.listFiles("work.issoh.co.jp/Diskstation/取込");

            listAndPrint(ftpClient,"/");
//            listAndPrint(ftpClient,"");
//            listAndPrint(ftpClient,"kowa share");
//            listAndPrint(ftpClient,"取込");
//            listAndPrint(ftpClient,"DiskStation");
//            listAndPrint(ftpClient,"取込/kowa");


            String rootPath = "torikomi";
            String subRootPath = "kowa/import/source/stock";

            FTPFile[] ftpFiles = ftpClient.listFiles(rootPath + "/" + subRootPath);
            logger.debug("list " + (rootPath + "/" + subRootPath) + " result is " + ftpFiles.length );

            for (int i = 0; i < ftpFiles.length; i++) {
                FTPFile ftpFile = ftpFiles[i];
                String curFolder = ftpFile.getName();

                List<String> ignoreNames = Arrays.asList(".", "..");
                if(ignoreNames.contains(curFolder)){
                    continue;
                }


                if (ftpFile.isDirectory()) {

                    String currDir = rootPath + "/" + subRootPath + "/" + curFolder;

                    FTPFile[] ftpFiles1 = ftpClient.listFiles(currDir);
                    for (int i1 = 0; i1 < ftpFiles1.length; i1++) {
                        FTPFile needFile = ftpFiles1[i1];

                        if (needFile.isFile()) {
                            String fileName = needFile.getName();
                            logger.debug("find file " + fileName);

                            String localPath = Constants.ROOT_DIR + "/" + subRootPath + "/" + curFolder + "/" + needFile.getName();
                            String remotePath = rootPath + "/" + subRootPath + "/" + curFolder + "/" + needFile.getName();
                            OutputStream outputStream = new FileOutputStream(localPath);

                            boolean b = ftpClient.retrieveFile(remotePath, outputStream);
                            logger.debug("get file "+ fileName +"from server " + b);
                            boolean b1 = ftpClient.deleteFile(remotePath);
                            logger.debug("remove file "+ fileName +"from server " + b1);

                        }

                    }
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void listAndPrint(FTPClient client,String path){
        try {
            FTPFile[] ftpFiles = client.listFiles(path);

            logger.debug(path +" children length is " + ftpFiles.length);
            for (int i = 0; i < ftpFiles.length; i++) {
                String name = ftpFiles[i].getName();

                System.out.println("name is : "+ name);
                System.out.println("a:"+ name.length());
                System.out.println("A:"+ name.getBytes().length);
                System.out.println("B:" + name.getBytes("ISO-8859-1").length);
                System.out.println("C:" + name.getBytes("Shift_JIS").length);
                System.out.println("D:" + name.getBytes("UTF-8").length);

//                if(name.length() == 2){
//                    System.out.println("--");
//                    listAndPrint(client,path + "/"+ name);
//                }

                System.out.println("E:" + new String(name.getBytes("UTF-8")));
                byte [] arr1 = name.getBytes("UTF-8");
                byte [] arr2 = "取込".getBytes("UTF-8");


                StringBuilder sb1 = new StringBuilder();
                StringBuilder sb2 = new StringBuilder();
                for (int i1 = 0; i1 < arr1.length; i1++) {
                    sb1.append(arr1[i1]).append(",");
                }
                for (int i1 = 0; i1 < arr2.length; i1++) {
                    sb2.append(arr2[i1]).append(",");
                }
                System.out.println(sb1.toString());
                System.out.println(sb2.toString());

                System.out.println(ftpFiles[i].getName());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


}
