using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

internal static class Launcher
{
    [STAThread]
    private static void Main()
    {
        string projectRoot = AppDomain.CurrentDomain.BaseDirectory;
        string scriptPath = Path.Combine(projectRoot, "run.ps1");

        if (!File.Exists(scriptPath))
        {
            MessageBox.Show(
                "run.ps1 was not found next to Battleship.exe.",
                "Battleship",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
            return;
        }

        string powershellPath = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.System),
            @"WindowsPowerShell\v1.0\powershell.exe");

        var processStart = new ProcessStartInfo
        {
            FileName = powershellPath,
            Arguments = "-ExecutionPolicy Bypass -NoProfile -WindowStyle Hidden -File \"" + scriptPath + "\"",
            WorkingDirectory = projectRoot,
            UseShellExecute = false,
            CreateNoWindow = true
        };

        try
        {
            Process.Start(processStart);
        }
        catch (Exception ex)
        {
            MessageBox.Show(
                "Unable to start Battleship.\r\n\r\n" + ex.Message,
                "Battleship",
                MessageBoxButtons.OK,
                MessageBoxIcon.Error);
        }
    }
}
