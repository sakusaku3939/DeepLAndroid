package main

import (
	"archive/zip"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
)

func main() {
	zipFile := "DeepLAndroid.zip"
	destDir := "DeepLAndroid-build"

	fmt.Println("Extracting", zipFile, "...")
	if err := unzip(zipFile, destDir); err != nil {
		fmt.Printf("Error extracting zip: %v\n", err)
		return
	}

	// Assuming the zip extracts into a root folder like DeepLAndroid-master
	files, err := os.ReadDir(destDir)
	if err != nil {
		fmt.Printf("Error reading destDir: %v\n", err)
		return
	}
	if len(files) == 0 {
		fmt.Println("Error: No files found in extracted directory")
		return
	}

	// Try to find gradlew
	buildDir := filepath.Join(destDir, files[0].Name())
	if _, err := os.Stat(filepath.Join(buildDir, "gradlew")); os.IsNotExist(err) {
		// Just in case it extracted directly
		buildDir = destDir
	}

	fmt.Println("Building Android App in", buildDir, "...")

	var cmd *exec.Cmd
	// Use gradlew.bat for Windows
	if _, err := os.Stat(filepath.Join(buildDir, "gradlew.bat")); err == nil {
		cmd = exec.Command("cmd", "/C", "gradlew.bat", "assembleDebug")
	} else {
		cmd = exec.Command("sh", "gradlew", "assembleDebug")
	}
	cmd.Dir = buildDir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	if err := cmd.Run(); err != nil {
		fmt.Printf("Error running gradle build: %v\n", err)
		return
	}

	apkPath := filepath.Join(buildDir, "app", "build", "outputs", "apk", "debug", "app-debug.apk")
	outApk := "DeepLAndroid-built.apk"

	fmt.Println("Copying APK to", outApk, "...")
	if err := copyFile(apkPath, outApk); err != nil {
		fmt.Printf("Error copying APK: %v\n", err)
		return
	}

	fmt.Println("Success! APK created at:", outApk)
}

func unzip(src, dest string) error {
	r, err := zip.OpenReader(src)
	if err != nil {
		return err
	}
	defer r.Close()

	for _, f := range r.File {
		fpath := filepath.Join(dest, f.Name)
		if f.FileInfo().IsDir() {
			os.MkdirAll(fpath, os.ModePerm)
			continue
		}

		if err = os.MkdirAll(filepath.Dir(fpath), os.ModePerm); err != nil {
			return err
		}

		outFile, err := os.OpenFile(fpath, os.O_WRONLY|os.O_CREATE|os.O_TRUNC, f.Mode())
		if err != nil {
			return err
		}

		rc, err := f.Open()
		if err != nil {
			return err
		}

		_, err = io.Copy(outFile, rc)
		outFile.Close()
		rc.Close()

		if err != nil {
			return err
		}
	}
	return nil
}

func copyFile(src, dst string) error {
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()

	out, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, in)
	if err != nil {
		return err
	}
	return out.Close()
}
