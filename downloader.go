package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
)

func main() {
	repoURL := "https://github.com/sakusaku3939/DeepLAndroid/archive/refs/heads/master.zip"
	outputFile := "DeepLAndroid.zip"

	fmt.Printf("Downloading %s...\n", repoURL)

	resp, err := http.Get(repoURL)
	if err != nil {
		fmt.Printf("Error downloading repository: %v\n", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		fmt.Printf("Error: bad status: %s\n", resp.Status)
		return
	}

	out, err := os.Create(outputFile)
	if err != nil {
		fmt.Printf("Error creating file: %v\n", err)
		return
	}
	defer out.Close()

	_, err = io.Copy(out, resp.Body)
	if err != nil {
		fmt.Printf("Error saving file: %v\n", err)
		return
	}

	fmt.Printf("Successfully downloaded to %s\n", outputFile)
}
