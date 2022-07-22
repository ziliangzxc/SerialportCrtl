package com.meidical.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author DasonYu
 * @date 2020/6/19  17:29
 * @descprition
 */
public class ShellUtils {

    public static final String COMMAND_SU = "su";
    public static final String COMMAND_SH = "sh";
    public static final String COMMAND_EXIT = "exit\n";
    public static final String COMMAND_LINE_END = "\n";


    private ShellUtils() {
        throw new AssertionError();
    }


    /**
     * 查看是否有了root权限
     *
     * @return
     */
    public static boolean checkRootPermission() {
        return execCommand("echo root", true, false).result == 0;
    }


    /**
     * 执行shell命令，默认返回结果
     *
     * @param command      command
     * @param 运行是否需要root权限
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String command, boolean isRoot) {
        return execCommand(new String[]{command}, isRoot, true);
    }


    /**
     * 执行shell命令，默认返回结果
     *
     * @param commands     command list
     * @param 运行是否需要root权限
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(List<String> commands,
                                            boolean isRoot) {
        return execCommand(
                commands == null ? null : commands.toArray(new String[]{}),
                isRoot, true);
    }


    /**
     * 执行shell命令，默认返回结果
     *
     * @param commands     command array
     * @param 运行是否需要root权限
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String[] commands, boolean isRoot) {
        return execCommand(commands, isRoot, true);
    }


    /**
     * execute shell command
     *
     * @param command         command
     * @param 运行是否需要root权限
     * @param isNeedResultMsg whether need result msg
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(String command, boolean isRoot,
                                            boolean isNeedResultMsg) {
        return execCommand(new String[]{command}, isRoot, isNeedResultMsg);
    }


    /**
     * execute shell commands
     *
     * @param commands     command list
     * @param 运行是否需要root权限
     * @param 是否需要返回运行结果
     * @return
     * @see ShellUtils#execCommand(String[], boolean, boolean)
     */
    public static CommandResult execCommand(List<String> commands,
                                            boolean isRoot, boolean isNeedResultMsg) {
        return execCommand(
                commands == null ? null : commands.toArray(new String[]{}),
                isRoot, isNeedResultMsg);
    }


    /**
     * execute shell commands
     *
     * @param commands     command array
     * @param 运行是否需要root权限
     * @param 是否需要返回运行结果
     * @return <ul>
     * <li>if isNeedResultMsg is false, {@link CommandResult#successMsg}
     * is null and {@link CommandResult#errorMsg} is null.</li>
     * <li>if {@link CommandResult#result} is -1, there maybe some
     * excepiton.</li>
     * </ul>
     */
    public static CommandResult execCommand(String[] commands, boolean isRoot,
                                            boolean isNeedResultMsg) {
        int result = -1;
        if (commands == null || commands.length == 0) {
            return new CommandResult(result, null, null);
        }


        Process process = null;
        BufferedReader successResult = null;
        BufferedReader errorResult = null;
        StringBuilder successMsg = null;
        StringBuilder errorMsg = null;


        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec(
                    isRoot ? COMMAND_SU : COMMAND_SH);
            os = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                if (command == null) {
                    continue;
                }


                // donnot use os.writeBytes(commmand), avoid chinese charset
                // error
                os.write(command.getBytes());
                os.writeBytes(COMMAND_LINE_END);
                os.flush();
            }
            os.writeBytes(COMMAND_EXIT);
            os.flush();


            result = process.waitFor();
            // get command result
            if (isNeedResultMsg) {
                successMsg = new StringBuilder();
                errorMsg = new StringBuilder();
                successResult = new BufferedReader(new InputStreamReader(
                        process.getInputStream()));
                errorResult = new BufferedReader(new InputStreamReader(
                        process.getErrorStream()));
                String s;
                while ((s = successResult.readLine()) != null) {
                    successMsg.append(s);
                }
                while ((s = errorResult.readLine()) != null) {
                    errorMsg.append(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (successResult != null) {
                    successResult.close();
                }
                if (errorResult != null) {
                    errorResult.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (process != null) {
                process.destroy();
            }
        }
        return new CommandResult(result, successMsg == null ? null
                : successMsg.toString(), errorMsg == null ? null
                : errorMsg.toString());
    }

    /**
     * 导出gpio，并且置为接收状态
     *
     * @param num gpio序号
     * @return
     */
    public static void exportGPIO(int num) {
        List<String> lists = new ArrayList<>();
        lists.add("cd /sys/class/gpio");
        lists.add("echo " + num + " > export");
        lists.add("cd gpio" + num);
        lists.add("echo out > direction");
        lists.add("echo 0 > value");
        ShellUtils.execCommand(lists, true);
    }

    public static class CommandResult {


        /**
         * 运行结果
         **/
        public int result;
        /**
         * 运行成功结果
         **/
        public String successMsg;
        /**
         * 运行失败结果
         **/
        public String errorMsg;


        public CommandResult(int result) {
            this.result = result;
        }


        public CommandResult(int result, String successMsg, String errorMsg) {
            this.result = result;
            this.successMsg = successMsg;
            this.errorMsg = errorMsg;
        }
    }
}
