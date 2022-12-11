import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

/**
 * 将当前 dedeCMS 升级到指定版本，指定的版本仅限于 > 5.7.75 (20180109)
 * 提示：只是把补丁文件覆盖到 dedeCMS 目录下，并非是系统升级。
 */
public class Recover {

    private static File dedeCMSFile = new File("./DedeCMS");

    private static String dedeCMSVersion = "5.7.74";
    private static int edition = 74;

    private static String version = null;

    private static File patchDir = new File("./patch");
    
    public static void main(String[] args) throws Exception {
        
        if (args.length == 0) {
            exit("请指定生成的 dedecms 版本!");
        } 

        version = args[0];

        int needEdition = Integer.valueOf(version.substring(version.lastIndexOf('.') + 1, version.length()));
        int distance = needEdition - edition;

        // 版本号在 5.7.74 <= x <= 5.7.102
        if (distance < 0 || distance > 28) {
            exit("版本号超出范围，请重新输入!");
        }

        // 根据输入的版本号参数筛选补丁文件名
        String[] patchAllName = patchDir.list(new FilenameFilter() {
            private int i = 0;

            @Override
            public boolean accept(File dir, String name) {

                return (distance > (i++));
            }
        });

        if (patchAllName.length == 0) {
            exit("5.7.74 版本不需要打补丁，程序退出");
        }

        // 打补丁
        ZipInputStream zis = null;
        ZipEntry zipEntry = null;
        String name = null;
        File fileOfCms = null;

        BufferedOutputStream bos = null;
        byte[] bytes = new byte[1024];
        int len = 0;

        for (int i = 0; i < patchAllName.length; i++) {

            zis = new ZipInputStream(new FileInputStream(new File(patchDir, patchAllName[i])));

            while ((zipEntry = zis.getNextEntry()) != null) {

                name = zipEntry.getName();

                if (name.equals("utf-8/") || !name.startsWith("utf-8/")) {
                    continue;
                }

                name = name.substring(name.indexOf('/') + 1, name.length());
                fileOfCms = new File(dedeCMSFile, name);

                if (zipEntry.isDirectory()) {
                    if (!fileOfCms.exists()) {
                        fileOfCms.mkdir();
                    }
                } else {
                    // 先创建好目录，再创建文件
                    if (!fileOfCms.exists()) {
                        File parentDir = new File(fileOfCms.getParent());
                        if (!parentDir.exists()) {
                            parentDir.mkdirs();
                        }
                        fileOfCms.createNewFile();
                    }
                    bos = new BufferedOutputStream(new FileOutputStream(fileOfCms));
                    while ((len = zis.read(bytes)) != -1) {
                        bos.write(bytes, 0, len);
                    }

                    bos.flush();
                }
            } 
        }

        // 修改版本号
        String lastPatchName = patchAllName[patchAllName.length - 1].substring(0, 8);
        bos = new BufferedOutputStream(new FileOutputStream(new File(dedeCMSFile, "/data/admin/ver.txt")));
        bos.write(lastPatchName.getBytes());
        bos.flush();

        // 关闭流
        bos.close();
        zis.close();
    }

    // 显示信息并退出程序
    private static void exit(String message) {
        System.out.println(message);
        System.exit(0);
    }
}