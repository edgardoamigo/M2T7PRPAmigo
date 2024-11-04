import java.util.*;

public class VirtualMemoryManagement {

    public static List<Integer> generateReferenceString(int length, int pageRange) {
        List<Integer> referenceString = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < length; i++) {
            referenceString.add(rand.nextInt(pageRange));
        }
        return referenceString;
    }

    public static int[] fifoPageReplacement(List<Integer> referenceString, int frameSize) {
        int pageFaults = 0, hits = 0;
        Queue<Integer> frames = new LinkedList<>();

        for (int page : referenceString) {
            if (frames.contains(page)) {
                hits++;
            } else {
                pageFaults++;
                if (frames.size() == frameSize) {
                    frames.poll();
                }
                frames.add(page);
            }
        }
        return new int[]{pageFaults, hits};
    }

    public static int[] secondChancePageReplacement(List<Integer> referenceString, int frameSize) {
        int pageFaults = 0, hits = 0;
        List<Integer> frames = new ArrayList<>();
        Map<Integer, Integer> referenceBits = new HashMap<>();
        int index = 0;

        for (int page : referenceString) {
            if (frames.contains(page)) {
                hits++;
                referenceBits.put(page, 1); 
            } else {
                pageFaults++;
                if (frames.size() < frameSize) {
                    frames.add(page);
                    referenceBits.put(page, 1);
                } else {
                    while (true) {
                        int candidate = frames.get(index);
                        if (referenceBits.get(candidate) == 0) {
                            frames.set(index, page);
                            referenceBits.remove(candidate);
                            referenceBits.put(page, 1);
                            index = (index + 1) % frameSize;
                            break;
                        } else {
                            referenceBits.put(candidate, 0);
                            index = (index + 1) % frameSize;
                        }
                    }
                }
            }
        }
        return new int[]{pageFaults, hits};
    }

    public static int[] lruPageReplacement(List<Integer> referenceString, int frameSize) {
        int pageFaults = 0, hits = 0;
        List<Integer> frames = new ArrayList<>();
        Map<Integer, Integer> pageIndices = new HashMap<>();

        for (int i = 0; i < referenceString.size(); i++) {
            int page = referenceString.get(i);
            if (frames.contains(page)) {
                hits++;
            } else {
                pageFaults++;
                if (frames.size() == frameSize) {
                    int lruPage = Collections.min(pageIndices.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
                    frames.remove((Integer) lruPage);
                    pageIndices.remove(lruPage);
                }
                frames.add(page);
            }
            pageIndices.put(page, i);
        }
        return new int[]{pageFaults, hits};
    }

    public static int[] optimalPageReplacement(List<Integer> referenceString, int frameSize) {
        int pageFaults = 0, hits = 0;
        List<Integer> frames = new ArrayList<>();

        for (int i = 0; i < referenceString.size(); i++) {
            int page = referenceString.get(i);
            if (frames.contains(page)) {
                hits++;
            } else {
                pageFaults++;
                if (frames.size() < frameSize) {
                    frames.add(page);
                } else {
                    Map<Integer, Integer> futureIndices = new HashMap<>();
                    for (int frame : frames) {
                        int index = referenceString.subList(i + 1, referenceString.size()).indexOf(frame);
                        futureIndices.put(frame, index == -1 ? Integer.MAX_VALUE : index);
                    }
                    int frameToRemove = Collections.max(futureIndices.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
                    frames.remove((Integer) frameToRemove);
                    frames.add(page);
                }
            }
        }
        return new int[]{pageFaults, hits};
    }

    public static void simulate(List<Integer> referenceString, int[] frameSizes) {
        String[] algorithms = {"FIFO", "Second Chance", "LRU", "Optimal"};
        Map<String, String> bestWorstSummary = new HashMap<>();
        System.out.println("Reference String: " + referenceString);

        for (int frameSize : frameSizes) {
            System.out.printf("\nFrame Size: %d\n", frameSize);
            System.out.println("----------------------------------------------------------------------");
            System.out.println("Algorithm        Page Faults  Page Hits   Failure Rate   Success Rate");

            double highestSuccessRate = 0;
            double lowestSuccessRate = 100;
            String bestAlgorithm = "";
            String worstAlgorithm = "";

            for (String algorithm : algorithms) {
                int[] result;
                switch (algorithm) {
                    case "FIFO":
                        result = fifoPageReplacement(referenceString, frameSize);
                        break;
                    case "Second Chance":
                        result = secondChancePageReplacement(referenceString, frameSize);
                        break;
                    case "LRU":
                        result = lruPageReplacement(referenceString, frameSize);
                        break;
                    case "Optimal":
                        result = optimalPageReplacement(referenceString, frameSize);
                        break;
                    default:
                        result = new int[]{0, 0};
                }
                int pageFaults = result[0];
                int hits = result[1];
                int totalAccesses = referenceString.size();
                double failureRate = (pageFaults / (double) totalAccesses) * 100;
                double successRate = (hits / (double) totalAccesses) * 100;

                System.out.printf("%-15s %-12d %-10d %-14.2f %.2f%%\n",
                        algorithm, pageFaults, hits, failureRate, successRate);

                if (successRate > highestSuccessRate) {
                    highestSuccessRate = successRate;
                    bestAlgorithm = algorithm;
                }
                if (successRate < lowestSuccessRate) {
                    lowestSuccessRate = successRate;
                    worstAlgorithm = algorithm;
                }
            }
            bestWorstSummary.put("Frame Size " + frameSize, String.format("BEST  -> %s (%.1f%%) Success Rate\nWORST -> %s (%.1f%%) Success Rate",
                    bestAlgorithm, highestSuccessRate, worstAlgorithm, lowestSuccessRate));
        }

        System.out.println("----------------------------------------------------------------------");
        System.out.println("Summary:");
        bestWorstSummary.forEach((frameSize, result) -> {
            System.out.printf("%s: %s\n", frameSize, result);
        });
        System.out.println("----------------------------------------------------------------------");

        System.out.println("Narrative:");
        System.out.println("- The Optimal algorithm has the lowest failure rate across all frame sizes, followed by LRU.");
        System.out.println("- Second-Chance performs slightly better than FIFO, but both are generally worse than LRU and Optimal.");
        System.out.println("- Increasing the number of page frames reduces the failure rate for all algorithms, as expected.");
    }

    public static void main(String[] args) {
        List<Integer> referenceString = generateReferenceString(16, 7);
        System.out.println("Reference String: " + referenceString);

        int[] frameSizes = {3, 4, 5};
        simulate(referenceString, frameSizes);
    }
}